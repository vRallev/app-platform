package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestDispatcher
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
      @OptIn(ExperimentalStdlibApi::class)
      val coroutineDispatcher = coroutineScope.coroutineContext[ContinuationInterceptor.Key]
      if (coroutineDispatcher is TestDispatcher) {
        // If this is a TestDispatcher, then this scope was likely based on a TestScope
        // and we can create a wrapper. E.g. this happens if you do
        // `testScope + CoroutineName(..)`, which returns a CoroutineScope and not
        // TestScope.
        TestScope(coroutineScope.coroutineContext).moleculeScope(coroutineContext)
      } else {
        // Respect the choice not to use a TestScope, which also initializes the whole test
        // machinery with skipping.
        MoleculeScope(
          coroutineScope = coroutineScope + coroutineContext,
          recompositionMode = RecompositionMode.Immediate,
        )
      }
    }
  }
}
