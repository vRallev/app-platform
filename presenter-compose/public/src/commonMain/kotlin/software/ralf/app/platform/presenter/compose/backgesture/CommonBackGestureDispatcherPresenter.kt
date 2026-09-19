package software.ralf.app.platform.presenter.compose.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CommonBackGestureDispatcherPresenter : BackGestureDispatcherPresenter {
  private val listeners = mutableListOf<Listener>()

  private val _listenersCount = MutableStateFlow(0)
  override val listenersCount: StateFlow<Int> = _listenersCount

  override suspend fun onPredictiveBack(progress: Flow<BackEventPresenter>) {
    val listener =
      checkNotNull(listeners.lastOrNull { it.enabled }) {
        "No back gesture listener was registered or they were all disabled. " +
          "Check `listenerCount` before invoking this function."
      }

    listener.onBack(progress)
  }

  @Composable
  override fun PredictiveBackHandlerPresenter(
    enabled: Boolean,
    onBack: suspend (Flow<BackEventPresenter>) -> Unit,
  ) {
    // This implementation is somewhat inspired by
    // https://github.com/JetBrains/compose-multiplatform-core/blob/244635e202f9aa734bd8c86bd1748a9065ecd818/compose/ui/ui-backhandler/src/jbMain/kotlin/androidx/compose/ui/backhandler/BackHandler.jb.kt

    // Ensure we don't re-register callbacks when onBack changes. It's always the same `listener`
    // instance with its own callback that invokes the updated `onBack`.
    val currentOnBack by rememberUpdatedState(onBack)
    val listener = remember { Listener(enabled) { currentOnBack(it) } }

    LaunchedEffect(enabled) {
      listener.enabled = enabled
      updateListenersCount()
    }

    DisposableEffect(Unit) {
      listeners += listener
      updateListenersCount()

      onDispose {
        listeners.remove(listener)
        updateListenersCount()
      }
    }
  }

  private fun updateListenersCount() {
    _listenersCount.value = listeners.count { it.enabled }
  }

  private class Listener(
    var enabled: Boolean,
    val onBack: suspend (Flow<BackEventPresenter>) -> Unit,
  )
}
