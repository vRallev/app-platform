package software.ralf.app.platform.presenter.molecule

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

class TestMoleculeScopeTest {

  @Test
  fun `test recompositionMode of moleculeScope is always Immediate`() = runTest {
    var moleculeScope = moleculeScope()
    assertThat(moleculeScope.recompositionMode).isEqualTo(RecompositionMode.Immediate)

    moleculeScope = moleculeScope(CoroutineName("test"))
    assertThat(moleculeScope.recompositionMode).isEqualTo(RecompositionMode.Immediate)
  }

  @Test
  fun `a standard test dispatcher is used by default`() = runTest {
    val job =
      moleculeScope().coroutineScope.launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isFalse()
    runCurrent()
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `an unconfined test dispatcher can be used`() = runTest {
    val job =
      moleculeScope(UnconfinedTestDispatcher()).coroutineScope.launch {
        // Do nothing
      }
    assertThat(job.isCompleted).isTrue()
  }

  @Test
  fun `the coroutine context can be changed`() = runTest {
    val name =
      moleculeScope(CoroutineName("Test-abc"))
        .coroutineScope
        .coroutineContext[CoroutineName.Key]
        ?.name
    assertThat(name).isEqualTo("Test-abc")
  }

  @Test
  fun `the coroutine context is canceled`() = runTest {
    val moleculeScope = moleculeScope()
    assertThat(moleculeScope.coroutineScope.isActive).isTrue()
    moleculeScope.cancel()
    assertThat(moleculeScope.coroutineScope.isActive).isFalse()
  }

  @Test
  @IgnoreWasm
  fun `failures in a coroutine are reported`() {
    assertFailure {
        runTest {
          val moleculeScope = moleculeScope()
          moleculeScope.coroutineScope.launch { error("test failure") }

          runCurrent()
        }
      }
      .rootCause()
      .messageContains("test failure")
  }
}
