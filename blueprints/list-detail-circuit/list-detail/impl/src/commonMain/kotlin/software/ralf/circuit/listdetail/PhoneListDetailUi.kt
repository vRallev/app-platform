@file:OptIn(ExperimentalComposeUiApi::class)

package software.ralf.circuit.listdetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.foundation.rememberUi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.transform
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject

@Inject
class PhoneListDetailUi : Ui<PhoneListDetailPresenter.State> {
  @Composable
  override fun Content(state: PhoneListDetailPresenter.State, modifier: Modifier) {
    val circuit = checkNotNull(LocalCircuit.current)
    val latestState by rememberUpdatedState(state)
    val navigator = remember { BackEventNavigator { latestState } }
    val saveableStateHolder = rememberSaveableStateHolder()

    // CircuitX installs an inner predictive handler on supported platforms. This remains the
    // fallback elsewhere; the dispatcher invokes only the active handler for a back event.
    if (LocalNavigationEventDispatcherOwner.current != null) {
      NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.entries.backwardItems.any(),
        onBackCompleted = { state.eventSink(PhoneListDetailPresenter.Event.Back) },
      )
    }

    circuit.defaultNavDecoration.DecoratedContent(
      args = state.entries.transform { NavigationArgument(it) },
      navigator = navigator,
      modifier = modifier.fillMaxSize(),
    ) { argument ->
      // A popped entry remains visible until its exit animation finishes. Live entries use the
      // latest state, while a departing entry keeps its last snapshot for the transition.
      val lastEntry = remember(argument.key) { mutableStateOf(argument.entry) }
      val entry = latestState.entries.firstOrNull { it.key == argument.key } ?: lastEntry.value
      SideEffect { lastEntry.value = entry }
      saveableStateHolder.SaveableStateProvider(entry.key) {
        EntryContent(entry = entry, circuit = circuit)
      }
      DisposableEffect(entry.key) {
        onDispose {
          if (latestState.entries.none { it.key == entry.key }) {
            saveableStateHolder.removeState(entry.key)
          }
        }
      }
    }
  }

  @Composable
  private fun EntryContent(entry: PhoneListDetailPresenter.Entry, circuit: Circuit) {
    key(circuit) {
      val ui =
        checkNotNull(
          rememberUi(screen = entry.screen, factory = { screen, _ -> circuit.ui(screen) })
        ) {
          "No Circuit UI registered for ${entry.screen}."
        }
      ui.Content(entry.state, Modifier.fillMaxSize())
    }
  }

  private class BackEventNavigator(private val state: () -> PhoneListDetailPresenter.State) :
    Navigator by Navigator.NoOp {
    override fun pop(result: PopResult?): Screen? {
      state().eventSink(PhoneListDetailPresenter.Event.Back)
      return null
    }

    override fun peek(): Screen = state().entries.active.screen

    override fun peekBackStack(): List<Screen> =
      state().entries.let { entries ->
        listOf(entries.active.screen) + entries.backwardItems.map { it.screen }
      }

    override fun peekNavStack(): NavStackList<Screen> = state().entries.transform { it.screen }
  }

  // Circuit compares navigation arguments to distinguish pushes, pops, and root changes. State
  // updates must not change that identity or interfere with the active navigation transition.
  private class NavigationArgument(val entry: PhoneListDetailPresenter.Entry) : NavArgument {
    override val key: String
      get() = entry.key

    override val screen: Screen
      get() = entry.screen

    override fun equals(other: Any?): Boolean = other is NavigationArgument && key == other.key

    override fun hashCode(): Int = key.hashCode()
  }
}
