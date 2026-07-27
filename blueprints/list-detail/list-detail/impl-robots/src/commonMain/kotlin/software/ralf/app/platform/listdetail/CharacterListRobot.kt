package software.ralf.app.platform.listdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.zacsweers.metro.AppScope
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.ComposeRobot

/** Robot for assertions and interactions on the character list. */
@ContributesRobot(AppScope::class)
class CharacterListRobot : ComposeRobot() {
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
