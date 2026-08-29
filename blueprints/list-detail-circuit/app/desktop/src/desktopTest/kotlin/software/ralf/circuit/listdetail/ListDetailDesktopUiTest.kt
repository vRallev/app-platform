@file:OptIn(ExperimentalTestApi::class)

package software.ralf.circuit.listdetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import kotlin.test.Test

class ListDetailDesktopUiTest {
  private val uiTestRule = DesktopUiTestRule()

  @Test
  fun `phone opens character detail and returns to list`() = uiTestRule.runPhoneTest {
    val list = CharacterListRobot(this)
    val detail = CharacterDetailRobot(this)

    list.seeCharacterList()
    list.seeCharacter("samwise")
    list.selectCharacter("samwise")

    detail.seeCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
    detail.seeBackButton()
    detail.navigateBack()

    list.seeCharacterList()
    list.seeCharacter("samwise")
  }

  @Test
  fun `tablet updates detail without phone navigation`() = uiTestRule.runTabletTest {
    val list = CharacterListRobot(this)
    val detail = CharacterDetailRobot(this)

    list.seeCharacterList()
    list.seeCharacter("aragorn")
    list.seeCharacterSelected("frodo")
    detail.seeCharacter(name = "Frodo Baggins", ageAtRingDestruction = "50")
    detail.seeNoBackButton()

    list.selectCharacter("aragorn")

    list.seeCharacterList()
    list.seeCharacterSelected("aragorn")
    detail.seeCharacter(name = "Aragorn", ageAtRingDestruction = "88")
    detail.seeNoBackButton()
  }

  @Test
  fun `phone preserves list scroll and selection across repeated detail visits`() =
    uiTestRule.runPhoneTest {
      val list = CharacterListRobot(this)
      val detail = CharacterDetailRobot(this)

      onNode(hasScrollAction()).performScrollToNode(hasTestTag("character-pippin"))
      list.seeCharacter("pippin")
      list.selectCharacter("pippin")
      detail.seeCharacter(name = "Peregrin “Pippin” Took", ageAtRingDestruction = "28")
      detail.navigateBack()

      list.seeCharacter("pippin")
      list.seeCharacterSelected("pippin")
      list.selectCharacter("merry")
      detail.seeCharacter(name = "Meriadoc “Merry” Brandybuck", ageAtRingDestruction = "36")
      detail.navigateBack()

      list.seeCharacter("pippin")
      list.seeCharacterSelected("merry")
    }
}
