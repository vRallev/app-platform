package software.ralf.app.platform.presenter.molecule

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.internal.IgnoreWasm

class FakeMoleculeScopeFactoryTest {

  @Test
  fun `a created MoleculeScope can be canceled`() = runTest {
    val scope = FakeMoleculeScopeFactory(this).createMoleculeScope()
    scope.cancel()

    var didRun = false
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { didRun = true }

    assertThat(didRun).isTrue()
    assertThat(scope.coroutineScope.isActive).isFalse()
  }

  @Test
  @IgnoreWasm
  fun `a created MoleculeScope does not need to be canceled for the test to complete`() {
    // Basically this test should not hang.
    var didRun = false
    runTest {
      FakeMoleculeScopeFactory(this).createMoleculeScope()
      didRun = true
    }

    assertThat(didRun).isTrue()
  }

  @Test
  fun `the coroutine context is added to the scope`() = runTest {
    val scope =
      FakeMoleculeScopeFactory(this)
        .createMoleculeScopeFromCoroutineScope(this, CoroutineName("test"))

    val name = scope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")
  }

  @Test
  fun `a decorated TestScope does not create an unmanaged child`() = runTest {
    val testJob = coroutineContext.job
    val originalChildren = testJob.children.toList()

    FakeMoleculeScopeFactory(this)
      .createMoleculeScopeFromCoroutineScope(this + CoroutineName("decorated"))

    assertThat(testJob.children.toList()).isEqualTo(originalChildren)
  }

  @Test
  fun `a regular CoroutineScope can be used to create a MoleculeScope`() = runTest {
    val coroutineScope = CoroutineScope(CoroutineName("test"))
    val moleculeScope =
      FakeMoleculeScopeFactory(this).createMoleculeScopeFromCoroutineScope(coroutineScope)

    val name = moleculeScope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")

    moleculeScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }

  @Test
  fun `a regular CoroutineScope can be used to create a MoleculeScope without a TestScope`() {
    val coroutineScope = CoroutineScope(CoroutineName("test"))
    val moleculeScope = FakeMoleculeScopeFactory(coroutineScope).createMoleculeScope()

    val name = moleculeScope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")

    moleculeScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }
}
