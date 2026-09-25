package software.ralf.app.platform.scope.coroutine

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.onExit

class CoroutineScopeServiceTest {

  @Test
  fun `a coroutine scope can be registered in a scope`() {
    val coroutineScope = CoroutineScopeScoped(Job() + CoroutineName("abc"))

    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }

    assertThat(scope.coroutineScope().coroutineContext[CoroutineName]?.name).isEqualTo("abc-child")
  }

  @Test
  fun `a coroutine scope is canceled when the Scope is destroyed`() {
    val coroutineScope = CoroutineScopeScoped(Job() + CoroutineName("abc"))

    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }

    val childCoroutineScope = scope.coroutineScope()

    scope.destroy()
    assertThat(coroutineScope.isActive).isFalse()
    assertThat(childCoroutineScope.isActive).isFalse()
  }

  @Test
  fun `destruction waits for nested coroutine cleanup`() = runTest {
    val coroutineScope = CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("test"))
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }
    val cleanupStarted = CompletableDeferred<Unit>()
    val finishCleanup = CompletableDeferred<Unit>()
    var cleanupFinished = false
    var onExitCalled = false
    scope.onExit { onExitCalled = true }

    val childCoroutineScope = scope.coroutineScope()
    val job =
      childCoroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
        launch(start = CoroutineStart.UNDISPATCHED) {
          try {
            awaitCancellation()
          } finally {
            withContext(NonCancellable) {
              cleanupStarted.complete(Unit)
              finishCleanup.await()
              cleanupFinished = true
            }
          }
        }
      }

    val destruction = async(start = CoroutineStart.UNDISPATCHED) { scope.destroyAndWait() }
    cleanupStarted.await()

    assertThat(scope.isDestroyed()).isTrue()
    assertThat(onExitCalled).isTrue()
    assertThat(coroutineScope.coroutineContext.job.isCancelled).isTrue()
    assertThat(job.isCompleted).isFalse()
    assertThat(destruction.isCompleted).isFalse()

    finishCleanup.complete(Unit)
    destruction.await()

    assertThat(cleanupFinished).isTrue()
    assertThat(job.isCompleted).isTrue()
    assertThat(childCoroutineScope.coroutineContext.job.isCompleted).isTrue()
    assertThat(coroutineScope.coroutineContext.job.isCompleted).isTrue()
  }

  @Test
  fun `destruction waits for independent coroutine scopes in all descendants`() = runTest {
    val root = Scope.buildRootScope()
    val child = root.buildChild("child")
    val cleanedUp = mutableListOf<String>()
    val scopes =
      listOf(child, root).mapIndexed { index, parent ->
        val coroutineScope =
          CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("child-$index"))
        parent
          .buildChild("child-$index") { addCoroutineScopeScoped(coroutineScope) }
          .also { scope ->
            scope.launch {
              try {
                awaitCancellation()
              } finally {
                withContext(NonCancellable) {
                  delay(index + 1L)
                  cleanedUp += scope.name
                }
              }
            }
          }
      }
    testScheduler.runCurrent()

    root.destroyAndWait()

    assertThat(root.isDestroyed()).isTrue()
    assertThat(child.isDestroyed()).isTrue()
    scopes.forEach { assertThat(it.isDestroyed()).isTrue() }
    assertThat(cleanedUp).containsExactlyInAnyOrder("child-0", "child-1")
  }

  @Test
  fun `a scope with no coroutine jobs is destroyed`() = runTest {
    val coroutineScope = CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("test"))
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }

    scope.destroyAndWait()

    assertThat(scope.isDestroyed()).isTrue()
    assertThat(coroutineScope.coroutineContext.job.isCancelled).isTrue()
    assertThat(coroutineScope.coroutineContext.job.isCompleted).isTrue()
  }

  @Test
  fun `a scope without a coroutine scope is destroyed`() = runTest {
    val scope = Scope.buildRootScope()

    scope.destroyAndWait()

    assertThat(scope.isDestroyed()).isTrue()
  }

  @Test
  fun `destruction waits for cleanup in an already canceled coroutine scope`() = runTest {
    val coroutineScope = CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("test"))
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }
    var cleanupFinished = false
    val job =
      coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
          awaitCancellation()
        } finally {
          withContext(NonCancellable) {
            delay(1)
            cleanupFinished = true
          }
        }
      }
    coroutineScope.cancel()

    scope.destroyAndWait()

    assertThat(scope.isDestroyed()).isTrue()
    assertThat(cleanupFinished).isTrue()
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `destruction can be awaited again after caller cancellation`() = runTest {
    val coroutineScope = CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("test"))
    val scope = Scope.buildRootScope { addCoroutineScopeScoped(coroutineScope) }
    val finishCleanup = CompletableDeferred<Unit>()
    val job =
      coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
          awaitCancellation()
        } finally {
          withContext(NonCancellable) { finishCleanup.await() }
        }
      }

    val destruction = async(start = CoroutineStart.UNDISPATCHED) { scope.destroyAndWait() }
    destruction.cancelAndJoin()

    assertFailsWith<CancellationException> { destruction.await() }
    assertThat(scope.isDestroyed()).isTrue()
    assertThat(job.isCancelled).isTrue()
    assertThat(job.isCompleted).isFalse()

    val retriedDestruction = async(start = CoroutineStart.UNDISPATCHED) { scope.destroyAndWait() }
    assertThat(retriedDestruction.isCompleted).isFalse()

    finishCleanup.complete(Unit)
    retriedDestruction.await()

    assertThat(job.isCompleted).isTrue()
    scope.destroyAndWait()
  }

  @Test
  fun `an already destroyed scope cannot be awaited`() = runTest {
    val scope = Scope.buildRootScope()
    scope.destroy()

    assertFailsWith<IllegalStateException> { scope.destroyAndWait() }
  }
}
