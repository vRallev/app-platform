package software.ralf.app.platform.scope

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.scope.coroutine.coroutineScope

class RunTestWithScopeTest {

  @Test
  fun `a test scope comes with a coroutine scope`() = runTestWithScope { scope ->
    assertThat(scope.name).isEqualTo("test-root-scope")

    val coroutineScope = scope.coroutineScope()
    assertThat(coroutineScope.isActive).isTrue()
  }

  @Test
  fun `a standard test dispatcher is used by default`() = runTestWithScope {
    val job1 =
      it.coroutineScope().launch {
        // Do nothing
      }

    val job2 = launch {
      // No nothing
    }

    assertThat(job1.isCompleted).isFalse()
    assertThat(job2.isCompleted).isFalse()

    runCurrent()

    assertThat(job1.isCompleted).isTrue()
    assertThat(job2.isCompleted).isTrue()
  }

  @Test
  fun `the coroutine context can be changed`() =
    runTestWithScope(UnconfinedTestDispatcher()) {
      val job1 =
        it.coroutineScope().launch {
          // Do nothing
        }
      val job2 = launch {
        // Do nothing
      }

      assertThat(job1.isCompleted).isTrue()
      assertThat(job2.isCompleted).isTrue()
    }

  @Test
  @IgnoreWasm
  fun `onEnterScope and onExitScope will be called when registering the scoped`() {
    val scoped = MyScoped()
    assertThat(scoped.onEnterScopeCalled).isFalse()
    assertThat(scoped.onExitScopeCalled).isFalse()

    runTestWithScope { scope ->
      assertThat(scoped.onEnterScopeCalled).isFalse()
      assertThat(scoped.onExitScopeCalled).isFalse()

      scope.register(scoped)

      assertThat(scoped.onEnterScopeCalled).isTrue()
      assertThat(scoped.onExitScopeCalled).isFalse()
    }

    assertThat(scoped.onEnterScopeCalled).isTrue()
    assertThat(scoped.onExitScopeCalled).isTrue()
  }

  @Test
  @IgnoreWasm
  fun `onExitScope will not be called when calling onEnterScope manually`() {
    val scoped = MyScoped()
    assertThat(scoped.onEnterScopeCalled).isFalse()
    assertThat(scoped.onExitScopeCalled).isFalse()

    runTestWithScope { scope ->
      assertThat(scoped.onEnterScopeCalled).isFalse()
      assertThat(scoped.onExitScopeCalled).isFalse()

      scoped.onEnterScope(scope)

      assertThat(scoped.onEnterScopeCalled).isTrue()
      assertThat(scoped.onExitScopeCalled).isFalse()
    }

    assertThat(scoped.onEnterScopeCalled).isTrue()
    assertThat(scoped.onExitScopeCalled).isFalse()
  }

  @Test
  @IgnoreWasm
  fun `onEnterScope and onExitScope will be called when registering the scoped through runTestWithScoped`() {
    val scoped = MyScoped()
    assertThat(scoped.onEnterScopeCalled).isFalse()
    assertThat(scoped.onExitScopeCalled).isFalse()

    runTestWithScoped(scoped) {
      assertThat(scoped.onEnterScopeCalled).isTrue()
      assertThat(scoped.onExitScopeCalled).isFalse()
    }

    assertThat(scoped.onEnterScopeCalled).isTrue()
    assertThat(scoped.onExitScopeCalled).isTrue()
  }

  private class MyScoped : Scoped {
    var onEnterScopeCalled = false
      private set

    var onExitScopeCalled = false
      private set

    override fun onEnterScope(scope: Scope) {
      onEnterScopeCalled = true
    }

    override fun onExitScope() {
      onExitScopeCalled = true
    }
  }
}
