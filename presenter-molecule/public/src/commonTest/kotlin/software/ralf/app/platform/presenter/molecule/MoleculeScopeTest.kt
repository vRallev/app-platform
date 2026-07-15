package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

class MoleculeScopeTest {

  @Test
  fun `canceling a MoleculeScope cancels the CoroutineScope`() {
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    val moleculeScope = MoleculeScope(coroutineScope, RecompositionMode.Immediate)

    assertThat(coroutineScope.isActive).isTrue()

    moleculeScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }
}
