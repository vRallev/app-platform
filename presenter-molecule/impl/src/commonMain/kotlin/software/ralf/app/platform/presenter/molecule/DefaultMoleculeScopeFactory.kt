package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import software.ralf.app.platform.presenter.PresenterCoroutineScope

/**
 * Creates new [MoleculeScope]s with the given defaults. When calling [createMoleculeScope], then
 * [coroutineScopeFactory] is used as default scope. [coroutineContext] allows you to add additional
 * elements to created scopes. [recompositionMode] is used for launching [MoleculePresenter]s.
 */
internal class DefaultMoleculeScopeFactory(
  @PresenterCoroutineScope private val coroutineScopeFactory: () -> CoroutineScope,
  private val coroutineContext: CoroutineContext = EmptyCoroutineContext,
  private val recompositionMode: RecompositionMode,
) : MoleculeScopeFactory {

  override fun createMoleculeScope(): MoleculeScope =
    createMoleculeScopeFromCoroutineScope(coroutineScopeFactory())

  override fun createMoleculeScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
  ): MoleculeScope =
    MoleculeScope(
      coroutineScope = coroutineScope + this.coroutineContext + coroutineContext,
      recompositionMode = recompositionMode,
    )
}
