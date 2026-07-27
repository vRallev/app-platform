@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.listdetail.presenternavigation.DefaultBackstackModel
import software.ralf.app.platform.listdetail.presenternavigation.presenterBackstackDefault
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.requireNotNull
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.template.ModelDelegate

/**
 * Compact-screen presentation that models list-detail navigation as a presenter backstack.
 *
 * The list is the initial entry. Selecting a character pushes an assisted detail presenter, which
 * makes system and toolbar back navigation behave like a normal phone application.
 */
@Inject
class PhoneListDetailPresenter(
  private val listPresenter: CharacterListPresenter,
  private val detailPresenterFactory: CharacterDetailPresenter.Factory,
) : MoleculePresenter<PhoneListDetailPresenter.Input, PhoneListDetailPresenter.Model> {
  @Composable
  override fun present(input: Input): Model {
    val selectionState = input.selectionState
    val initialPresenter =
      remember(this, selectionState) {
        object : MoleculePresenter<Unit, CharacterListPresenter.Model> {
          @Composable
          override fun present(input: Unit): CharacterListPresenter.Model {
            return presentListModel(selectionState)
          }
        }
      }

    return Model(presenterBackstackDefault(initialPresenter))
  }

  @Composable
  private fun presentListModel(
    selectionState: ListDetailSelectionState
  ): CharacterListPresenter.Model {
    val backstackScope = LocalBackstackScope.requireNotNull()
    val listModel =
      listPresenter.present(
        CharacterListPresenter.Input(selectedCharacterId = selectionState.selectedCharacterId)
      )

    return listModel.copy(
      onCharacterSelected = { character ->
        selectionState.select(character.id)
        backstackScope.push(
          detailPresenterFactory.createCharacterDetailPresenter(
            characterId = character.id,
            showBackButton = true,
          )
        )
      }
    )
  }

  /** State supplied by the adaptive parent so selection survives layout changes. */
  data class Input(
    /** Shared character selection state. */
    val selectionState: ListDetailSelectionState
  )

  /** Delegates rendering to App Platform's presenter-backstack model. */
  data class Model(private val model: DefaultBackstackModel) : BaseModel, ModelDelegate {
    override fun delegate(): BaseModel = model
  }
}
