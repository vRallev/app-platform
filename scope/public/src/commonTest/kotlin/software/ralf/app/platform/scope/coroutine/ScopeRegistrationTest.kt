@file:OptIn(InternalCoroutinesApi::class)

package software.ralf.app.platform.scope.coroutine

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.register

class ScopeRegistrationTest {

  @Test
  fun `a batch defers scope helpers and previously created child coroutine scopes`() = runTest {
    val coroutines = coroutines()
    val injected = coroutines.createChild()
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutines) }
    val events = mutableListOf<String>()

    scope.register(
      listOf(
        scoped { current ->
          current.launch { events += "scope launch" }
          current.coroutineScope().launch { events += "coroutine scope launch" }
          @Suppress("DeferredResultUnused") injected.async { events += "injected async" }
          current.coroutineScope(Dispatchers.Unconfined + CoroutineName("child")).launch {
            assertThat(coroutineContext[CoroutineName]?.name).isEqualTo("child")
            events += "override"
          }
        },
        scoped {
          assertThat(events).isEmpty()
          events += "last callback"
        },
      )
    )

    assertThat(events)
      .containsExactly(
        "last callback",
        "scope launch",
        "coroutine scope launch",
        "injected async",
        "override",
      )
    scope.destroyAndWait()
  }

  @Test
  fun `a dispatching delegate cannot run work during registration`() = runTest {
    val scope = Scope.buildRootScope {
      addCoroutineScopeScoped(coroutines(StandardTestDispatcher(testScheduler)))
    }
    var ran = false

    scope.register(
      listOf(
        scoped { current -> current.coroutineScope().launch { ran = true } },
        scoped {
          // Let anything already submitted to the delegate run before registration finishes.
          // This catches early dispatch even though StandardTestDispatcher normally queues it.
          runCurrent()
          assertThat(ran).isFalse()
        },
      )
    )

    runCurrent()
    assertThat(ran).isTrue()
    scope.destroyAndWait()
  }

  @Test
  fun `root and child builders wait even when the coroutine service is added last`() = runTest {
    for (child in listOf(false, true)) {
      val root = Scope.buildRootScope()
      val coroutines = coroutines()
      val events = mutableListOf<String>()
      val builder: Scope.Builder.() -> Unit = {
        register(scoped { current -> current.launch { events += "coroutine" } })
        register(scoped { events += "last callback" })
        addCoroutineScopeScoped(coroutines)
      }
      val scope =
        if (child) root.buildChild("child", builder) else Scope.buildRootScope(builder = builder)

      assertThat(events).containsExactly("last callback", "coroutine")
      scope.destroyAndWait()
      root.destroyAndWait()
    }
  }

  @Test
  fun `nested batches wait for the outer batch`() = runTest {
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutines()) }
    val events = mutableListOf<String>()
    scope.register(
      listOf(
        scoped { current ->
          current.register(listOf(scoped { it.launch { events += "coroutine" } }))
          events += "outer callback"
        },
        scoped { events += "last callback" },
      )
    )

    assertThat(events).containsExactly("outer callback", "last callback", "coroutine")
    scope.destroyAndWait()
  }

  @Test
  fun `a failed batch releases the barrier without destroying the scope`() = runTest {
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutines()) }
    var ran = false
    val failure = IllegalStateException("registration failed")
    val caught =
      assertFailsWith<IllegalStateException> {
        scope.register(listOf(scoped { it.launch { ran = true } }, scoped { throw failure }))
      }

    assertThat(caught).isSameInstanceAs(failure)
    assertThat(ran).isTrue()
    assertThat(scope.isDestroyed()).isFalse()
    scope.destroyAndWait()
  }

  @Test
  fun `canceling queued work completes without canceling its siblings`() = runTest {
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutines()) }
    val events = mutableListOf<String>()
    lateinit var canceled: Job
    scope.register(
      listOf(
        scoped { current ->
          canceled = current.launch { events += "canceled" }
          canceled.cancel()
          current.launch { events += "sibling" }
        }
      )
    )

    assertThat(events).containsExactly("sibling")
    assertThat(canceled.isCompleted).isTrue()
    scope.destroyAndWait()
  }

  @Test
  fun `delay and timeout retain the delegate test dispatcher clock`() = runTest {
    val scope = Scope.buildRootScope {
      addCoroutineScopeScoped(coroutines(StandardTestDispatcher(testScheduler)))
    }
    val events = mutableListOf<String>()
    scope.register(
      listOf(
        scoped { current ->
          current.launch {
            delay(100.milliseconds)
            events += "delay"
            withTimeoutOrNull(100.milliseconds) { delay(200.milliseconds) }
            events += "timeout"
          }
        }
      )
    )

    runCurrent()
    advanceTimeBy(100.milliseconds)
    runCurrent()
    assertThat(events).containsExactly("delay")
    advanceTimeBy(100.milliseconds)
    runCurrent()
    assertThat(events).containsExactly("delay", "timeout")
    assertThat(testScheduler.currentTime).isEqualTo(200L)
    scope.destroyAndWait()
  }

  @Test
  fun `unconfined work retains its event loop and explicit starts bypass the barrier`() = runTest {
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutines(Dispatchers.Unconfined)) }
    var count = 0
    var bypassed = 0
    scope.register(
      listOf(
        scoped { current ->
          current.launch {
            repeat(10_000) {
              count++
              yield()
            }
          }
          current.coroutineScope().launch(Dispatchers.Unconfined) { bypassed++ }
          current.coroutineScope().launch(start = CoroutineStart.UNDISPATCHED) { bypassed++ }
          current.launch(start = CoroutineStart.UNDISPATCHED) { bypassed++ }
          assertThat(count).isEqualTo(0)
          assertThat(bypassed).isEqualTo(3)
        }
      )
    )

    assertThat(count).isEqualTo(10_000)
    val job = scope.launch { count++ }
    assertThat(job.isCompleted).isTrue()
    assertThat(count).isEqualTo(10_001)
    scope.destroyAndWait()
  }

  @Test
  fun `registration can finish between a scheduling decision and yield dispatch`() {
    val registration = RegistrationDispatcher.Registration()
    val dispatcher = RegistrationDispatcher.create(Dispatchers.Unconfined, registration)
    var ran = false

    registration.begin()
    assertThat(dispatcher.isDispatchNeeded(dispatcher)).isTrue()
    registration.end()
    dispatcher.dispatchYield(dispatcher) { ran = true }

    assertThat(ran).isTrue()
  }

  private fun TestScope.coroutines(
    context: CoroutineContext = UnconfinedTestDispatcher(testScheduler)
  ): CoroutineScopeScoped = CoroutineScopeScoped(Job() + CoroutineName("test") + context)

  private fun scoped(onEnter: (Scope) -> Unit): Scoped =
    object : Scoped {
      override fun onEnterScope(scope: Scope) = onEnter(scope)
    }
}
