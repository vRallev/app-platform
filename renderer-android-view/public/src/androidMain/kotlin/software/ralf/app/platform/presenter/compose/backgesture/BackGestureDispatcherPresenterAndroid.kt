package software.ralf.app.platform.presenter.compose.backgesture

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Registers a callback in the [OnBackPressedDispatcher] that is enabled as long as there is a
 * presenter with an enabled back handler. This function forwards back press events to presenters
 * until the lifecycle from [onBackPressedDispatcherOwner] is in the destroyed state.
 *
 * It's recommended to call this function from your Android `Activity`:
 * ```
 * class MainActivity : ComponentActivity() {
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *
 *     // Inject the dispatcher from the dependency injection graph.
 *     backGestureDispatcherPresenter.forwardBackPressEventsToPresenters(this)
 *
 *     ...
 *   }
 * }
 * ```
 */
public fun BackGestureDispatcherPresenter.forwardBackPressEventsToPresenters(
  onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner
) {
  // Later if needed we can consider limiting this to the STARTED lifecycle if needed with
  // repeatOnLifecycle API. For now we forward events until the lifecycle changes to DESTROYED.
  onBackPressedDispatcherOwner.lifecycleScope.launch {
    val forwarder =
      BackGestureForwarder(
        onBackPressedDispatcherOwner = onBackPressedDispatcherOwner,
        dispatcherPresenter = this@forwardBackPressEventsToPresenters,
      )
    forwarder.forwardEvents()
  }
}

private class BackGestureForwarder(
  private val onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner,
  private val dispatcherPresenter: BackGestureDispatcherPresenter,
) {
  // This function never returns and this is by design. We stop listening to updates when the
  // coroutine get canceled.
  suspend fun forwardEvents(): Nothing = coroutineScope {
    val backCallBack = createOnBackPressCallback(this)
    try {
      onBackPressedDispatcherOwner.onBackPressedDispatcher.addCallback(
        onBackPressedDispatcherOwner,
        backCallBack,
      )

      dispatcherPresenter.listenersCount.collect { count -> backCallBack.isEnabled = count > 0 }
    } finally {
      backCallBack.remove()
    }
  }

  private fun createOnBackPressCallback(coroutineScope: CoroutineScope): OnBackPressedCallback {
    val enabled = dispatcherPresenter.listenersCount.value > 0

    // This implementation comes mainly from
    // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity-compose/src/main/java/androidx/activity/compose/PredictiveBackHandler.kt
    return object : OnBackPressedCallback(enabled) {
      var onBackInstance: OnBackInstance? = null

      override fun handleOnBackStarted(backEvent: BackEventCompat) {
        super.handleOnBackStarted(backEvent)
        // in case the previous onBackInstance was started by a normal back gesture
        // we want to make sure it's still cancelled before we start a predictive
        // back gesture
        onBackInstance?.cancel()
        onBackInstance = OnBackInstance(coroutineScope, true) { onBack(it) }
      }

      override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        super.handleOnBackProgressed(backEvent)
        onBackInstance?.send(backEvent)
      }

      override fun handleOnBackPressed() {
        // handleOnBackPressed could be called by regular back to restart
        // a new back instance. If this is the case (where current back instance
        // was NOT started by handleOnBackStarted) then we need to reset the previous
        // regular back.
        onBackInstance?.run {
          if (!isPredictiveBack) {
            cancel()
            onBackInstance = null
          }
        }
        if (onBackInstance == null) {
          onBackInstance = OnBackInstance(coroutineScope, false) { onBack(it) }
        }

        // finally, we close the channel to ensure no more events can be sent
        // but let the job complete normally
        onBackInstance?.close()
      }

      override fun handleOnBackCancelled() {
        super.handleOnBackCancelled()
        // cancel will purge the channel of any sent events that are yet to be received
        onBackInstance?.cancel()
      }
    }
  }

  private suspend fun onBack(events: Flow<BackEventCompat>) {
    dispatcherPresenter.onPredictiveBack(
      events.map {
        BackEventPresenter(
          touchX = it.touchX,
          touchY = it.touchY,
          progress = it.progress,
          swipeEdge = it.swipeEdge,
        )
      }
    )
  }
}

private class OnBackInstance(
  scope: CoroutineScope,
  val isPredictiveBack: Boolean,
  onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) {
  val channel = Channel<BackEventCompat>(capacity = BUFFERED, onBufferOverflow = SUSPEND)
  val job = scope.launch {
    var completed = false
    onBack(channel.consumeAsFlow().onCompletion { completed = true })
    check(completed) { "You must collect the progress flow" }
  }

  fun send(backEvent: BackEventCompat) = channel.trySend(backEvent)

  // idempotent if invoked more than once
  fun close() = channel.close()

  fun cancel() {
    channel.cancel(CancellationException("onBack cancelled"))
    job.cancel()
  }
}
