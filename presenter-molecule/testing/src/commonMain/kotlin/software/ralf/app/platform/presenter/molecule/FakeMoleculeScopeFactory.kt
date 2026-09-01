package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope

/**
 * Uses the given [coroutineScope] to create new [MoleculeScope] instances. In testing environments
 * often [TestScope] is used as argument.
 */
public class FakeMoleculeScopeFactory(private val coroutineScope: CoroutineScope) :
  MoleculeScopeFactory {
  override fun createMoleculeScope(): MoleculeScope =
    createMoleculeScopeFromCoroutineScope(coroutineScope)

  override fun createMoleculeScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
  ): MoleculeScope {
    return if (coroutineScope is TestScope) {
      coroutineScope.moleculeScope(coroutineContext)
    } else {
      MoleculeScope(
        coroutineScope = coroutineScope + coroutineContext,
        recompositionMode = RecompositionMode.Immediate,
      )
    }
  }
}
