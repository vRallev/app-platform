package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class TabletListDetailPresenter(
  private val listPresenterFactory: CharacterListPresenter.Factory,
  private val detailPresenterFactory: CharacterDetailPresenter.Factory,
  @Assisted private val selectionState: ListDetailSelectionState,
) : Presenter<TabletListDetailPresenter.State> {
  @Composable
  override fun present(): State {
    val selectedCharacterId = selectionState.selectedCharacterId
    val navigator = remember(selectionState) { SelectionNavigator(selectionState) }
    val listScreen = CharacterListScreen(selectedCharacterId)
    val listPresenter =
      remember(listScreen, navigator) {
        listPresenterFactory.create(listScreen, navigator)
      }
    val listState = listPresenter.present()
    val selectedCharacter =
      listState.characters.firstOrNull { it.id == selectedCharacterId }
        ?: listState.characters.firstOrNull()
    val effectiveSelectedCharacterId = selectedCharacter?.id

    LaunchedEffect(effectiveSelectedCharacterId, selectedCharacterId) {
      if (
        selectionState.selectedCharacterId == selectedCharacterId &&
          selectedCharacterId != effectiveSelectedCharacterId
      ) {
        selectionState.select(effectiveSelectedCharacterId)
      }
    }

    val effectiveListState = listState.copy(selectedCharacterId = effectiveSelectedCharacterId)
    if (selectedCharacter == null) {
      return State.Empty(effectiveListState)
    }

    val detailState =
      key(selectedCharacter.id) {
        val screen = CharacterDetailScreen(selectedCharacter.id, showBackButton = false)
        val presenter =
          remember(screen, navigator) {
            detailPresenterFactory.create(screen, navigator)
          }
        presenter.present()
      }

    return State.Content(effectiveListState, detailState)
  }

  sealed interface State : CircuitUiState {
    val listState: CharacterListPresenter.State

    data class Empty(override val listState: CharacterListPresenter.State) : State

    data class Content(
      override val listState: CharacterListPresenter.State,
      val detailState: CharacterDetailPresenter.State,
    ) : State
  }

  @AssistedFactory
  fun interface Factory {
    fun create(selectionState: ListDetailSelectionState): TabletListDetailPresenter
  }

  private class SelectionNavigator(private val selectionState: ListDetailSelectionState) :
    Navigator by Navigator.NoOp {
    override fun goTo(screen: Screen): Boolean {
      if (screen !is CharacterDetailScreen) return false
      selectionState.select(screen.characterId)
      return true
    }
  }
}
