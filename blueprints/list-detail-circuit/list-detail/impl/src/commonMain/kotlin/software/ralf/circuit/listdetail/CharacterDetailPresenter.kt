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

/** Resolves the screen's ID on every repository update, including edits and removal. */
@AssistedInject
class CharacterDetailPresenter(
  private val repository: CharacterRepository,
  @Assisted private val screen: CharacterDetailScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<CharacterDetailPresenter.State> {
  @Composable
  override fun present(): State {
    val characters by repository.characters.collectAsState()
    val characterState =
      characters.firstOrNull { it.id == screen.characterId }?.let { DetailState.Available(it) }
        ?: DetailState.Missing(screen.characterId)
    val eventSink: (Event) -> Unit =
      remember(navigator, screen.showBackButton) {
        { event ->
          when (event) {
            Event.Back -> if (screen.showBackButton) navigator.pop()
          }
        }
      }

    return State(characterState, screen.showBackButton, eventSink)
  }

  data class State(
    val state: DetailState,
    val showBackButton: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface DetailState {
    data class Available(val character: Character) : DetailState

    data class Missing(val characterId: String) : DetailState
  }

  sealed interface Event : CircuitUiEvent {
    data object Back : Event
  }

  @AssistedFactory
  fun interface Factory {
    fun create(screen: CharacterDetailScreen, navigator: Navigator): CharacterDetailPresenter
  }
}
