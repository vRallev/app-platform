package software.ralf.app.platform.presenter.molecule

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

/** Creates new [MoleculeScope] instances. */
public interface MoleculeScopeFactory {

  /**
   * Creates a new [MoleculeScope]. Once the returned scope is not needed anymore, you must call
   * [MoleculeScope.cancel] to avoid memory leaks.
   */
  public fun createMoleculeScope(): MoleculeScope

  /**
   * Wraps the given [coroutineScope] in a [MoleculeScope] and applies platform specific defaults in
   * order to run Molecule. [coroutineContext] allows you to add additional elements to the used
   * [CoroutineScope] and override the platform defaults if necessary.
   */
  public fun createMoleculeScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ): MoleculeScope
}
