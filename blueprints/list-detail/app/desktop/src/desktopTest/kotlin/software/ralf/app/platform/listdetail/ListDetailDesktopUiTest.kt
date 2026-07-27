@file:OptIn(ExperimentalTestApi::class)

package software.ralf.app.platform.listdetail

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import software.ralf.app.platform.robot.composeRobot
import software.ralf.app.platform.robot.waitUntilCatching

/** Desktop integration tests for phone navigation and the adaptive tablet presentation. */
class ListDetailDesktopUiTest {
  private val uiTestRule = DesktopUiTestRule()

  @Test
  fun `phone opens character detail and returns to list`() = uiTestRule.runPhoneRobotTest {
    waitUntilCatching("character list displayed", timeout = 3.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacter("samwise")
      }
    }

    composeRobot<CharacterListRobot> { selectCharacter("samwise") }

    waitUntilCatching("Samwise detail displayed", timeout = 3.seconds) {
      composeRobot<CharacterDetailRobot> {
        seeCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
        seeBackButton()
      }
    }

    composeRobot<CharacterDetailRobot> { navigateBack() }

    waitUntilCatching("character list restored", timeout = 3.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacter("samwise")
      }
    }
  }

  @Test
  fun `tablet updates detail without phone navigation`() = uiTestRule.runTabletRobotTest {
    waitUntilCatching("tablet list-detail displayed", timeout = 3.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacter("aragorn")
        seeCharacterSelected("frodo")
      }
      composeRobot<CharacterDetailRobot> {
        seeCharacter(name = "Frodo Baggins", ageAtRingDestruction = "50")
        seeNoBackButton()
      }
    }

    composeRobot<CharacterListRobot> { selectCharacter("aragorn") }

    waitUntilCatching("Aragorn detail displayed", timeout = 3.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacterSelected("aragorn")
      }
      composeRobot<CharacterDetailRobot> {
        seeCharacter(name = "Aragorn", ageAtRingDestruction = "88")
        seeNoBackButton()
      }
    }
  }
}
