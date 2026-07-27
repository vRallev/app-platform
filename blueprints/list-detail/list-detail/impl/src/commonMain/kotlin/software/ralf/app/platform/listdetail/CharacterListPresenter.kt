package software.ralf.app.platform.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/** Converts the repository's observable data into immutable character-list render state. */
@Inject
class CharacterListPresenter(private val repository: CharacterRepository) :
  MoleculePresenter<CharacterListPresenter.Input, CharacterListPresenter.Model> {
  @Composable
  override fun present(input: Input): Model {
    val characters by repository.characters.collectAsState()

    return Model(
      characters = characters,
      selectedCharacterId = input.selectedCharacterId,
    )
  }

  /** Selection owned by the parent phone or tablet presenter. */
  data class Input(
    /** Identifier highlighted by the list renderer, or `null` when nothing is selected. */
    val selectedCharacterId: String?
  )

  /**
   * Render state for the character list.
   *
   * [onCharacterSelected] is supplied by the parent presenter so the reusable list presenter does
   * not need to know whether selection means pushing navigation or updating a detail pane.
   */
  data class Model(
    /** Characters displayed in repository order. */
    val characters: List<Character>,
    /** Identifier of the character currently highlighted in the list. */
    val selectedCharacterId: String?,
    /** Reports a user selection to the parent presentation. */
    val onCharacterSelected: (Character) -> Unit = {},
  ) : BaseModel
}
