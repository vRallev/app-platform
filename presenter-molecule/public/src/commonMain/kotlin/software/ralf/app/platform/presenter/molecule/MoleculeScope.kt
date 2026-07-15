package software.ralf.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * A pair of a [CoroutineScope] and a Compose [RecompositionMode] to make it easier to launch a
 * [MoleculePresenter]. Once a [MoleculeScope] is no longer used it must be canceled through
 * [cancel] otherwise [coroutineScope] will leak.
 */
public class MoleculeScope(
  /** The CoroutineScope which this MoleculeScope should use to run @Composable functions. */
  public val coroutineScope: CoroutineScope,

  /**
   * The [RecompositionMode] which this MoleculeScope should use to determine how frequently new
   * models are computed.
   */
  public val recompositionMode: RecompositionMode,
) {

  /** Cancel the provided [coroutineScope]. */
  public fun cancel(): Unit = coroutineScope.cancel()
}
