package software.ralf.app.platform.listdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.zacsweers.metro.AppScope
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.ComposeRobot

/** Robot for assertions and interactions on character details. */
@ContributesRobot(AppScope::class)
class CharacterDetailRobot : ComposeRobot() {
  /** Verifies the visible character detail content. */
  fun seeCharacter(name: String, ageAtRingDestruction: String) {
    compose.onNodeWithTag("characterDetail").assertIsDisplayed()
    compose.onNodeWithTag("characterDetailName").assertTextEquals(name)
    compose
      .onNodeWithTag("characterDetailAge")
      .assertTextEquals("Age when the Ring was destroyed: $ageAtRingDestruction")
  }

  /** Verifies that back navigation is visible. */
  fun seeBackButton() {
    compose.onNodeWithTag("detailBack").assertIsDisplayed()
  }

  /** Verifies that the detail does not expose back navigation. */
  fun seeNoBackButton() {
    compose.onNodeWithTag("detailBack").assertDoesNotExist()
  }

  /** Uses the detail toolbar to return to the list. */
  fun navigateBack() {
    compose.onNodeWithTag("detailBack").assertIsDisplayed().performClick()
  }
}
