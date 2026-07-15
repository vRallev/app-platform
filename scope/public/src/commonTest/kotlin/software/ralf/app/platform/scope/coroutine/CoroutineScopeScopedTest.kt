package software.ralf.app.platform.scope.coroutine

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineScopeScopedTest {

  @Test
  fun `the provided context must have a name for easier debugging`() {
    assertFailsWith<IllegalArgumentException> { CoroutineScopeScoped(EmptyCoroutineContext) }

    CoroutineScopeScoped(Job() + CoroutineName("abc"))
  }

  @Test
  fun `onExitScope cancels the CoroutineContext`() {
    val scope = CoroutineScopeScoped(Job() + CoroutineName("abc"))
    scope.onExitScope()

    assertThat(scope.coroutineContext.isActive).isFalse()
  }

  @Test
  fun `a child scope has a meaningful default name`() {
    val scope = CoroutineScopeScoped(Job() + CoroutineName("abc"))
    val child = scope.createChild()
    val name = child.coroutineContext[CoroutineName]?.name

    assertThat(name).isEqualTo("abc-child")
  }

  @Test
  fun `a child scope uses the provided name`() {
    val scope = CoroutineScopeScoped(Job() + CoroutineName("abc"))
    val child = scope.createChild(CoroutineName("def"))
    val name = child.coroutineContext[CoroutineName]?.name

    assertThat(name).isEqualTo("def")
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test
  fun `a child scope can use a different dispatcher`() {
    val scope = CoroutineScopeScoped(Job() + StandardTestDispatcher() + CoroutineName("abc"))
    val child = scope.createChild(UnconfinedTestDispatcher())

    assertThat(scope.coroutineContext[ContinuationInterceptor.Key].toString())
      .contains("StandardTestDispatcher")
    assertThat(child.coroutineContext[ContinuationInterceptor.Key].toString())
      .contains("UnconfinedTestDispatcher")
  }

  @Test
  fun `the child scope is canceled when the parent is canceled`() {
    val scope = CoroutineScopeScoped(Job() + CoroutineName("abc"))
    val child = scope.createChild()

    assertThat(scope.isActive).isTrue()
    assertThat(child.isActive).isTrue()

    scope.cancel()
    assertThat(scope.isActive).isFalse()
    assertThat(child.isActive).isFalse()
  }

  @Test
  fun `the parent scope is not canceled when the child is canceled`() {
    val scope = CoroutineScopeScoped(Job() + CoroutineName("abc"))
    val child = scope.createChild()

    assertThat(scope.isActive).isTrue()
    assertThat(child.isActive).isTrue()

    child.cancel()
    assertThat(scope.isActive).isTrue()
    assertThat(child.isActive).isFalse()
  }

  @Test
  fun `the CoroutineScope is canceled before any other Scoped instance`() = runTest {
    val scope = Scope.buildRootScope()
    val exitScopeOrder = mutableListOf<String>()

    val coroutineScope =
      CoroutineScopeScoped(Job() + CoroutineName("abc") + UnconfinedTestDispatcher())

    coroutineScope.launch {
      try {
        delay(10)
      } catch (_: CancellationException) {
        exitScopeOrder += "coroutine"
      }
    }

    scope.register(
      object : Scoped {
        override fun onExitScope() {
          exitScopeOrder += "first"
        }
      }
    )

    scope.register(coroutineScope)

    scope.register(
      object : Scoped {
        override fun onExitScope() {
          exitScopeOrder += "third"
        }
      }
    )

    scope.destroy()

    assertThat(exitScopeOrder).containsExactly("coroutine", "first", "third")
  }
}
