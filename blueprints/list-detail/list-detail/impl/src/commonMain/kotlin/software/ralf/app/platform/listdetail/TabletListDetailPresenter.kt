package software.ralf.app.platform.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Expanded-screen presentation that emits list and detail state simultaneously.
 *
 * It initializes an absent or invalid selection from the first available character, keeping the
 * list highlight and detail pane consistent when data or window size changes.
 */
@Inject
class TabletListDetailPresenter(
  private val listPresenter: CharacterListPresenter,
  private val detailPresenterFactory: CharacterDetailPresenter.Factory,
) : MoleculePresenter<TabletListDetailPresenter.Input, TabletListDetailPresenter.Model> {
  @Composable
  override fun present(input: Input): Model {
    val selectionState = input.selectionState
    val selectedCharacterId = selectionState.selectedCharacterId
    val listModel =
      listPresenter.present(CharacterListPresenter.Input(selectedCharacterId = selectedCharacterId))
    val selectedCharacter =
      listModel.characters.firstOrNull { it.id == selectedCharacterId }
        ?: listModel.characters.firstOrNull()
    val effectiveSelectedCharacterId = selectedCharacter?.id

    LaunchedEffect(effectiveSelectedCharacterId, selectedCharacterId) {
      if (
        selectionState.selectedCharacterId == selectedCharacterId &&
          selectedCharacterId != effectiveSelectedCharacterId
      ) {
        selectionState.select(effectiveSelectedCharacterId)
      }
    }

    val model =
      listModel.copy(
        selectedCharacterId = effectiveSelectedCharacterId,
        onCharacterSelected = { character -> selectionState.select(character.id) },
      )
    if (selectedCharacter == null) {
      return Model.Empty(listModel = model)
    }

    val detailPresenter =
      remember(selectedCharacter.id, detailPresenterFactory) {
        detailPresenterFactory.createCharacterDetailPresenter(
          characterId = selectedCharacter.id,
          showBackButton = false,
        )
      }

    return Model.Content(
      listModel = model,
      detailModel = detailPresenter.present(Unit),
    )
  }

  /** State supplied by the adaptive parent so selection survives layout changes. */
  data class Input(
    /** Shared character selection state. */
    val selectionState: ListDetailSelectionState
  )

  /** States understood by the tablet renderer. */
  sealed interface Model : BaseModel {
    /** List state always rendered in the leading pane. */
    val listModel: CharacterListPresenter.Model

    /** Represents an empty repository, where no detail presenter can be created. */
    data class Empty(override val listModel: CharacterListPresenter.Model) : Model

    /** Represents a selected character and the detail rendered in the trailing pane. */
    data class Content(
      override val listModel: CharacterListPresenter.Model,
      /** Detail state for the effective selection. */
      val detailModel: CharacterDetailPresenter.Model,
    ) : Model
  }
}
