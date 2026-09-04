package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * A pair of a [CoroutineScope] and a Compose [RecompositionMode] to make it easier to launch a
 * [ComposePresenter]. Once a [ComposePresenterScope] is no longer used it must be canceled through
 * [cancel] otherwise [coroutineScope] will leak.
 */
public class ComposePresenterScope(
  /**
   * The CoroutineScope which this ComposePresenterScope should use to run @Composable functions.
   */
  public val coroutineScope: CoroutineScope,

  /**
   * The [RecompositionMode] which this ComposePresenterScope should use to determine how frequently
   * new models are computed.
   */
  public val recompositionMode: RecompositionMode,
) {

  /** Cancel the provided [coroutineScope]. */
  public fun cancel(): Unit = coroutineScope.cancel()
}
