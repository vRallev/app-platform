package software.ralf.circuit.listdetail

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/** Robot for assertions and interactions on the character list. */
class CharacterListRobot(private val compose: SemanticsNodeInteractionsProvider) {
  /** Verifies that the character list is visible. */
  fun seeCharacterList() {
    compose.onNodeWithTag("characterList").assertIsDisplayed()
  }

  /** Verifies that the row identified by [characterId] is visible. */
  fun seeCharacter(characterId: String) {
    compose.onNodeWithTag("character-$characterId").assertIsDisplayed()
  }

  /** Verifies that the row identified by [characterId] is exposed as selected. */
  fun seeCharacterSelected(characterId: String) {
    compose.onNodeWithTag("character-$characterId").assertIsSelected()
  }

  /** Selects the visible row identified by [characterId]. */
  fun selectCharacter(characterId: String) {
    compose.onNodeWithTag("character-$characterId").assertIsDisplayed().performClick()
  }
}
