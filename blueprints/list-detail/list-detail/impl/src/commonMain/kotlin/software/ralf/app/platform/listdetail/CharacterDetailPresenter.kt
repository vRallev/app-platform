@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Presents live detail state for one character ID.
 *
 * [showBackButton] separates phone navigation, where detail is pushed onto a backstack, from the
 * persistent tablet detail pane. Looking up [characterId] on every repository emission keeps both
 * navigation modes synchronized with data changes.
 */
@AssistedInject
class CharacterDetailPresenter(
  private val repository: CharacterRepository,
  @Assisted private val characterId: String,
  @Assisted private val showBackButton: Boolean,
) : MoleculePresenter<Unit, CharacterDetailPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstackScope = LocalBackstackScope.current
    val characters by repository.characters.collectAsState()
    val state =
      characters.firstOrNull { it.id == characterId }?.let { State.Available(it) }
        ?: State.Missing(characterId)

    return Model(
      state = state,
      showBackButton = showBackButton,
      onBack = { if (showBackButton) backstackScope?.pop() },
    )
  }

  /** Renderable character detail state with navigation behavior supplied by the presenter. */
  data class Model(
    /** Current repository state for the requested character ID. */
    val state: State,
    /** Whether the renderer should expose back navigation. */
    val showBackButton: Boolean,
    /** Pops the phone backstack when back navigation is available. */
    val onBack: () -> Unit,
  ) : BaseModel

  /** Character lookup states understood by the detail renderer. */
  sealed interface State {
    /** The requested character is present in the latest repository value. */
    data class Available(
      /** Character rendered by the detail screen. */
      val character: Character
    ) : State

    /** The requested character is no longer present in the repository. */
    data class Missing(
      /** Identifier that could not be resolved. */
      val characterId: String
    ) : State
  }

  /** Creates a detail presenter for an assisted character ID and navigation mode. */
  @AssistedFactory
  interface Factory {
    /** Creates the presenter used for [characterId] in the requested navigation mode. */
    fun createCharacterDetailPresenter(
      characterId: String,
      showBackButton: Boolean,
    ): CharacterDetailPresenter
  }
}
