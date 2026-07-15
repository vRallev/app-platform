package software.ralf.app.platform.scope

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import assertk.assertions.rootCause
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.scope.coroutine.coroutineScope
import software.ralf.app.platform.scope.coroutine.launch

class TestScopeTest {

  @Test
  fun `a test scope comes with a coroutine scope`() = runTest {
    val scope = Scope.buildTestScope(this, name = "abc")
    assertThat(scope.name).isEqualTo("abc")

    val coroutineScope = scope.coroutineScope()
    assertThat(coroutineScope.isActive).isTrue()

    scope.destroy()
    assertThat(coroutineScope.isActive).isFalse()
  }

  @Test
  fun `a standard test dispatcher is used by default`() = runTest {
    val job =
      Scope.buildTestScope(this).coroutineScope().launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isFalse()

    runCurrent()

    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `an unconfined test dispatcher can be used`() = runTest {
    val job =
      Scope.buildTestScope(this, context = UnconfinedTestDispatcher()).launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `the coroutine context can be changed`() = runTest {
    val name =
      Scope.buildTestScope(this, context = CoroutineName("Test-abc"))
        .coroutineScope()
        .coroutineContext[CoroutineName.Key]
        ?.name
    assertThat(name).isEqualTo("Test-abc-child")
  }

  @Test
  @IgnoreWasm
  fun `the test scope is destroyed automatically`() {
    lateinit var scope: Scope

    runTest {
      scope = Scope.buildTestScope(this)
      assertThat(scope.isDestroyed()).isFalse()
    }

    assertThat(scope.isDestroyed()).isTrue()
  }

  @Test
  @IgnoreWasm
  fun `a failure in the clean up routine causes the test to fail`() {
    assertFailure {
        runTest {
          val scope = Scope.buildTestScope(this)
          scope.onExit { error("test failure") }
        }
      }
      .rootCause()
      .messageContains("test failure")
  }
}
