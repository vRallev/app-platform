package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.foundation.Navigator
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.transform
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/** Presents the real Circuit navigation stack without requiring a Compose UI host. */
@AssistedInject
class PhoneListDetailPresenter(
  private val listPresenterFactory: CharacterListPresenter.Factory,
  private val detailPresenterFactory: CharacterDetailPresenter.Factory,
  private val circuitSaver: CircuitSaver,
  @Assisted private val selectionState: ListDetailSelectionState,
) : Presenter<PhoneListDetailPresenter.State> {
  @Composable
  override fun present(): State {
    val navStack = rememberSaveableNavStack(CharacterListScreen(), circuitSaver = circuitSaver)
    val navigator =
      remember(navStack, selectionState) {
        SelectionNavigator(Navigator(navStack, onRootPop = {}), selectionState)
      }
    val snapshot = checkNotNull(navStack.snapshot())
    val entries = snapshot.associate { record ->
      val screen =
        when (val destination = record.screen) {
          is CharacterListScreen ->
            destination.copy(selectedCharacterId = selectionState.selectedCharacterId)
          else -> destination
        }
      val state =
        key(record.key) {
          when (screen) {
            is CharacterListScreen -> {
              val presenter =
                remember(screen, navigator) {
                  listPresenterFactory.create(screen, navigator)
                }
              presenter.present()
            }
            is CharacterDetailScreen -> {
              val presenter =
                remember(screen, navigator) {
                  detailPresenterFactory.create(screen, navigator)
                }
              presenter.present()
            }
            else -> error("Unsupported list-detail screen: $screen")
          }
        }
      record.key to Entry(record.key, screen, state)
    }
    val eventSink: (Event) -> Unit =
      remember(navigator) {
        { event ->
          when (event) {
            Event.Back -> navigator.pop()
          }
        }
      }

    return State(snapshot.transform { entries.getValue(it.key) }, eventSink)
  }

  data class State(
    val entries: NavStackList<Entry>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  data class Entry(
    override val key: String,
    override val screen: Screen,
    val state: CircuitUiState,
  ) : NavArgument

  sealed interface Event : CircuitUiEvent {
    data object Back : Event
  }

  @AssistedFactory
  fun interface Factory {
    fun create(selectionState: ListDetailSelectionState): PhoneListDetailPresenter
  }

  private class SelectionNavigator(
    private val delegate: Navigator,
    private val selectionState: ListDetailSelectionState,
  ) : Navigator by delegate {
    override fun goTo(screen: Screen): Boolean = Snapshot.withMutableSnapshot {
      val navigated = delegate.goTo(screen)
      if (navigated && screen is CharacterDetailScreen) {
        selectionState.select(screen.characterId)
      }
      navigated
    }
  }
}
