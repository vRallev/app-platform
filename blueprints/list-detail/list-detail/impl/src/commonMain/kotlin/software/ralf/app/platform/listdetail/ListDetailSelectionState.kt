package software.ralf.app.platform.listdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Composition-owned state shared by the phone and tablet presenters. */
class ListDetailSelectionState {
  /** Stable identifier of the current selection, or `null` before selection is initialized. */
  var selectedCharacterId: String? by mutableStateOf(null)
    private set

  /** Updates the selection observed by whichever adaptive presenter is active. */
  fun select(characterId: String?) {
    selectedCharacterId = characterId
  }
}
