package software.ralf.app.platform.presenter.molecule.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * A dispatcher that forwards back press events from the UI layer to presenters. Internally it
 * manages a list of listeners and determines which listener is actively handling back gesture
 * events.
 *
 * This class facilitates managing multiple back gesture event listeners, allowing the most recently
 * added and enabled listener to handle events.
 *
 * An implementation of this interface is provided by App Platform and it's safe to inject this type
 * within the App scope.
 */
public interface BackGestureDispatcherPresenter {
  /**
   * The count of enabled listeners. The UI layer should observe this count and enable the back
   * gesture callback if its greater than 0 and disable the callback for a count of 0.
   */
  public val listenersCount: StateFlow<Int>

  /**
   * This is the callback for the UI layer to forward back press events to presenters when they
   * happen. The [progress] Flow will be consumed by presenters.
   */
  public suspend fun onPredictiveBack(progress: Flow<BackEventPresenter>)

  /**
   * Presenters call this function to register the [onBack] callback for back press gestures. See
   * [software.ralf.app.platform.presenter.molecule.backgesture.PredictiveBackHandlerPresenter] for
   * more details.
   */
  @Composable
  public fun PredictiveBackHandlerPresenter(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventPresenter>) -> Unit,
  )

  public companion object {

    /**
     * Creates a new [BackGestureDispatcherPresenter] instance.
     *
     * Usually, calling this function is not necessary and a default instance of
     * [BackGestureDispatcherPresenter] is provided by App Platform that can be injected, e.g.
     *
     * ```
     * @Inject
     * class RootPresenter(
     *   private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
     *   ...
     * )
     * ```
     *
     * This provided instance is a singleton that can be shared between presenters and renderers.
     *
     * This function [createNewInstance] is helpful if you need multiple instances that you want to
     * manage yourself.
     */
    public fun createNewInstance(): BackGestureDispatcherPresenter =
      CommonBackGestureDispatcherPresenter()
  }
}

/**
 * Provides the instance of [BackGestureDispatcherPresenter] to the presenter hierarchy. This value
 * is used by [PredictiveBackHandlerPresenter] and [BackHandlerPresenter]. If the value is not
 * provided, then these functions will throw an error when used.
 *
 * This composition local is usually registered in the root presenter of the presenter hierarchy,
 * e.g.
 *
 * ```
 * @Inject
 * class RootPresenter(
 *   private val backPressDispatcherPresenter: BackPressDispatcherPresenter,
 * ) : MoleculePresenter<Unit, Model> {
 *   @Composable
 *   override fun present(input: Unit): Model {
 *     return returningCompositionLocalProvider(
 *       LocalBackPressDispatcherPresenter provides backPressDispatcherPresenter
 *     ) {
 *       ... // Call other presenters.
 *     }
 *   }
 * }
 * ```
 */
public val LocalBackGestureDispatcherPresenter:
  ProvidableCompositionLocal<BackGestureDispatcherPresenter?> =
  compositionLocalOf {
    null
  }

/**
 * An effect for handling predictive system back gestures.
 *
 * Calling this in your presenter adds the given [onBack] lambda to the
 * [BackGestureDispatcherPresenter]. The lambda passes in a `Flow<BackEventPresenter>` where each
 * [BackEventPresenter] reflects the progress of current gesture back. The lambda content should
 * follow this structure:
 * ```
 * PredictiveBackHandlerPresenter { progress: Flow<BackEventCompat> ->
 *   // code for gesture back started
 *   try {
 *     progress.collect { backevent ->
 *       // code for progress
 *     }
 *     // code for completion
 *   } catch (e: CancellationException) {
 *     // code for cancellation
 *   }
 * }
 * ```
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the
 * call to system back and invoke its lambda. The call will continue to propagate up until it finds
 * an enabled BackHandler.
 *
 * **Important:** Back gestures can only be handled if a [BackGestureDispatcherPresenter] is
 * provided as composition local in the presenter hierarchy. See
 * [LocalBackGestureDispatcherPresenter] for more details. Further, back gestures need to be
 * forwarded from the UI layer to the [BackGestureDispatcherPresenter], e.g. using
 * `BackGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()`.
 *
 * @param enabled if this BackHandler should be enabled, true by default.
 * @param onBack the action invoked by back gesture.
 */
// Note that the receiver parameter is used to ensure that this function is only called within a
// presenter. This will make sure that a renderer doesn't call this function accidentally.
@Suppress("UnusedReceiverParameter")
@Composable
public fun MoleculePresenter<*, *>.PredictiveBackHandlerPresenter(
  enabled: Boolean = true,
  onBack: suspend (progress: Flow<BackEventPresenter>) -> Unit,
) {
  val dispatcher =
    checkNotNull(LocalBackGestureDispatcherPresenter.current) {
      "Couldn't find the BackGestureDispatcherPresenter in the presenter hierarchy. " +
        "Did you register the BackGestureDispatcherPresenter instance as composition " +
        "local? See LocalBackGestureDispatcherPresenter for more details."
    }
  dispatcher.PredictiveBackHandlerPresenter(enabled, onBack)
}

/**
 * An effect for handling the back event.
 *
 * Calling this in your presenter adds the given lambda to the [BackGestureDispatcherPresenter].
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the
 * call to system back and invoke its lambda. The call will continue to propagate up until it finds
 * an enabled BackHandler.
 *
 * **Important:** Back gestures can only be handled if a [BackGestureDispatcherPresenter] is
 * provided as composition local in the presenter hierarchy. See
 * [LocalBackGestureDispatcherPresenter] for more details. Further, back gestures need to be
 * forwarded from the UI layer to the [BackGestureDispatcherPresenter], e.g. using
 * `BackGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()`.
 *
 * @param enabled if this BackHandler should be enabled
 * @param onBack the action invoked by system back event
 */
// Note that the receiver parameter is used to ensure that this function is only called within a
// presenter. This will make sure that a renderer doesn't call this function accidentally.
@Composable
public fun MoleculePresenter<*, *>.BackHandlerPresenter(
  enabled: Boolean = true,
  onBack: () -> Unit,
) {
  PredictiveBackHandlerPresenter(enabled) { progress ->
    try {
      progress.collect { /*ignore*/ }
      onBack()
    } catch (_: CancellationException) {
      // ignore
    }
  }
}
