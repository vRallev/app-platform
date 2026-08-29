package software.ralf.circuit.listdetail

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/** Selection shared by the two layouts, owned by the root screen's composition. */
@Stable
class ListDetailSelectionState(selectedCharacterId: String? = null) {
  var selectedCharacterId: String? by mutableStateOf(selectedCharacterId)
    private set

  fun select(characterId: String?) {
    selectedCharacterId = characterId
  }

  companion object {
    val Saver =
      listSaver<ListDetailSelectionState, String?>(
        save = { listOf(it.selectedCharacterId) },
        restore = { ListDetailSelectionState(it.single()) },
      )
  }
}
