package software.ralf.circuit.listdetail

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/** Robot for assertions and interactions on character details. */
class CharacterDetailRobot(private val compose: SemanticsNodeInteractionsProvider) {
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
