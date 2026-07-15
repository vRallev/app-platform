package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * Creates and returns a [MoleculeScope] with a recompositionMode of [RecompositionMode.Immediate]
 * and with a scope that defaults to using [StandardTestDispatcher].
 *
 * @param coroutineContext a [CoroutineContext] to override any element of coroutine scope.
 */
public fun TestScope.moleculeScope(
  coroutineContext: CoroutineContext = EmptyCoroutineContext
): MoleculeScope {
  val scope = backgroundScope + CoroutineName("TestMoleculeScope") + coroutineContext

  return MoleculeScope(scope, RecompositionMode.Immediate)
}
