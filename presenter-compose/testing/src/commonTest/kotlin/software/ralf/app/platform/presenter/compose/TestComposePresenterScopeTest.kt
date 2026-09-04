package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
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

class TestComposePresenterScopeTest {

  @Test
  fun `test recompositionMode of composePresenterScope is always Immediate`() = runTest {
    var composePresenterScope = composePresenterScope()
    assertThat(composePresenterScope.recompositionMode).isEqualTo(RecompositionMode.Immediate)

    composePresenterScope = composePresenterScope(CoroutineName("test"))
    assertThat(composePresenterScope.recompositionMode).isEqualTo(RecompositionMode.Immediate)
  }

  @Test
  fun `a standard test dispatcher is used by default`() = runTest {
    val job =
      composePresenterScope().coroutineScope.launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isFalse()
    runCurrent()
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `an unconfined test dispatcher can be used`() = runTest {
    val job =
      composePresenterScope(UnconfinedTestDispatcher()).coroutineScope.launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `the coroutine context can be changed`() = runTest {
    val name =
      composePresenterScope(CoroutineName("Test-abc"))
        .coroutineScope
        .coroutineContext[CoroutineName.Key]
        ?.name
    assertThat(name).isEqualTo("Test-abc")
  }

  @Test
  fun `the coroutine context is canceled`() = runTest {
    val composePresenterScope = composePresenterScope()
    assertThat(composePresenterScope.coroutineScope.isActive).isTrue()
    composePresenterScope.cancel()
    assertThat(composePresenterScope.coroutineScope.isActive).isFalse()
  }

  @Test
  @IgnoreWasm
  fun `failures in a coroutine are reported`() {
    assertFailure {
        runTest {
          val composePresenterScope = composePresenterScope()
          composePresenterScope.coroutineScope.launch { error("test failure") }

          runCurrent()
        }
      }
      .rootCause()
      .messageContains("test failure")
  }
}
