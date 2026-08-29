package software.ralf.circuit.listdetail

import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.Rule
import org.junit.Test

class ListDetailAndroidUiTest {
  @get:Rule val uiTestRule = AndroidUiTestRule()

  @Test
  fun opens_character_detail_and_returns_to_list() = uiTestRule.runTest {
    openSamwiseDetail().navigateBack()

    CharacterListRobot(this).apply {
      seeCharacterList()
      seeCharacter("samwise")
    }
  }

  @Test
  fun selected_detail_survives_recreation_and_system_back_returns_to_list() = uiTestRule.runTest {
    openSamwiseDetail()

    uiTestRule.recreateActivity()

    CharacterDetailRobot(this).apply {
      seeCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
      seeBackButton()
    }

    uiTestRule.pressSystemBack()

    CharacterListRobot(this).apply {
      seeCharacterList()
      seeCharacter("samwise")
      seeCharacterSelected("samwise")
    }
  }

  private fun ComposeTestRule.openSamwiseDetail(): CharacterDetailRobot {
    CharacterListRobot(this).apply {
      seeCharacterList()
      seeCharacter("samwise")
      selectCharacter("samwise")
    }

    return CharacterDetailRobot(this).apply {
      seeCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
      seeBackButton()
    }
  }
}
