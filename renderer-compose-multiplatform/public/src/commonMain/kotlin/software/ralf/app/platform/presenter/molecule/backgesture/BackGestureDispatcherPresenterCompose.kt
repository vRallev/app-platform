package software.ralf.app.platform.presenter.molecule.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import software.ralf.app.platform.renderer.ComposeRenderer

/**
 * Registers a callback in the `BackGestureDispatcher` that is enabled as long as there is a
 * presenter with an enabled back handler.
 *
 * It's recommended to call this function from your root [ComposeRenderer], e.g.
 *
 * ```
 * @Inject
 * @ContributesRenderer
 * class RootPresenterRenderer(
 *   private val rendererFactory: RendererFactory,
 *   private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
 * ) : ComposeRenderer<Model>() {
 *
 *   @Composable
 *   override fun Compose(model: Model) {
 *     backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()
 *
 *     ...
 *   }
 * ```
 */
@Composable
public fun BackGestureDispatcherPresenter.ForwardBackPressEventsToPresenters() {
  val count by listenersCount.collectAsState()
  val navState = rememberNavigationEventState(NavigationEventInfo.None)
  var activeGestureChannel by remember { mutableStateOf<Channel<BackEventPresenter>?>(null) }
  val scope = rememberCoroutineScope()

  fun ensureGestureSession(): Channel<BackEventPresenter> {
    activeGestureChannel?.let {
      return it
    }
    val channel = Channel<BackEventPresenter>(Channel.BUFFERED)
    activeGestureChannel = channel
    scope.launch(start = CoroutineStart.UNDISPATCHED) { onPredictiveBack(channel.consumeAsFlow()) }
    return channel
  }

  NavigationBackHandler(
    state = navState,
    isBackEnabled = count > 0,
    onBackCompleted = {
      ensureGestureSession().close()
      activeGestureChannel = null
    },
    onBackCancelled = {
      activeGestureChannel?.cancel(CancellationException("Back gesture cancelled"))
      activeGestureChannel = null
    },
  )

  // Observe transition state and forward progress events
  LaunchedEffect(Unit) {
    snapshotFlow { navState.transitionState }
      .collect { transitionState ->
        if (transitionState is NavigationEventTransitionState.InProgress) {
          val channel = ensureGestureSession()

          val event = transitionState.latestEvent
          channel.trySend(
            BackEventPresenter(
              touchX = event.touchX,
              touchY = event.touchY,
              progress = event.progress,
              swipeEdge = event.swipeEdge,
            )
          )
        }
      }
  }

  DisposableEffect(Unit) {
    onDispose {
      activeGestureChannel?.cancel(CancellationException("Disposed"))
      activeGestureChannel = null
    }
  }
}
