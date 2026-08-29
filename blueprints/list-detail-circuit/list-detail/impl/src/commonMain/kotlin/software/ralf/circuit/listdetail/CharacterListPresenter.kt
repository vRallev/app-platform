package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class CharacterListPresenter(
  private val repository: CharacterRepository,
  @Assisted private val screen: CharacterListScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<CharacterListPresenter.State> {
  @Composable
  override fun present(): State {
    val characters by repository.characters.collectAsState()
    val eventSink: (Event) -> Unit =
      remember(navigator) {
        { event ->
          when (event) {
            is Event.SelectCharacter -> navigator.goTo(CharacterDetailScreen(event.characterId))
          }
        }
      }

    return State(characters, screen.selectedCharacterId, eventSink)
  }

  data class State(
    val characters: List<Character>,
    val selectedCharacterId: String?,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class SelectCharacter(val characterId: String) : Event
  }

  @AssistedFactory
  fun interface Factory {
    fun create(screen: CharacterListScreen, navigator: Navigator): CharacterListPresenter
  }
}
