package software.ralf.app.platform.listdetail

import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.robot.composeRobot
import software.ralf.app.platform.robot.waitUntilCatching

/** Android integration tests for phone navigation on the configured managed device. */
class ListDetailAndroidUiTest {
  /** Launches the production activity with the test graph and robot registrations. */
  @get:Rule val uiTestRule = AndroidUiTestRule()

  @Test
  fun opens_character_detail_and_returns_to_list() = uiTestRule.runRobotTest {
    waitUntilCatching("character list displayed", timeout = 5.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacter("samwise")
      }
    }

    composeRobot<CharacterListRobot> { selectCharacter("samwise") }

    waitUntilCatching("Samwise detail displayed", timeout = 5.seconds) {
      composeRobot<CharacterDetailRobot> {
        seeCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
        seeBackButton()
      }
    }

    composeRobot<CharacterDetailRobot> { navigateBack() }

    waitUntilCatching("character list restored", timeout = 5.seconds) {
      composeRobot<CharacterListRobot> {
        seeCharacterList()
        seeCharacter("samwise")
      }
    }
  }
}
