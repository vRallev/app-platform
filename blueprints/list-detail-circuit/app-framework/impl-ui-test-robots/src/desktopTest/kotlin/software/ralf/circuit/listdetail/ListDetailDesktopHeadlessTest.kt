package software.ralf.circuit.listdetail

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.circuit.test.CircuitReceiveTurbine
import kotlin.test.Test

class ListDetailDesktopHeadlessTest {
  private val headlessTestRule = DesktopHeadlessTestRule()

  @Test
  fun `phone opens character detail and returns to list`() = headlessTestRule.runPhoneTest {
    val listState = awaitPhoneList()
    listState.requireCharacter("samwise")
    listState.selectCharacter("samwise")

    val detailState = awaitPhoneDetail()
    detailState.assertCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
    assertThat(detailState.showBackButton).isTrue()

    detailState.eventSink(CharacterDetailPresenter.Event.Back)

    awaitPhoneList().requireCharacter("samwise")
  }

  @Test
  fun `tablet updates detail without phone navigation`() = headlessTestRule.runTabletTest {
    val initialState = awaitTabletContent()

    initialState.listState.requireCharacter("aragorn")
    assertThat(initialState.listState.selectedCharacterId).isEqualTo("frodo")
    initialState.detailState.assertCharacter(
      name = "Frodo Baggins",
      ageAtRingDestruction = "50",
    )
    assertThat(initialState.detailState.showBackButton).isFalse()

    initialState.listState.selectCharacter("aragorn")

    val updatedState = awaitTabletContent(selectedCharacterId = "aragorn")

    assertThat(updatedState.listState.selectedCharacterId).isEqualTo("aragorn")
    updatedState.detailState.assertCharacter(name = "Aragorn", ageAtRingDestruction = "88")
    assertThat(updatedState.detailState.showBackButton).isFalse()
  }

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitPhoneList():
    CharacterListPresenter.State {
    val phoneState = awaitPhoneState(size = 1)
    assertThat(phoneState.entries.active.screen).isInstanceOf<CharacterListScreen>()

    return requireInstance(phoneState.entries.active.state)
  }

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitPhoneDetail():
    CharacterDetailPresenter.State {
    val phoneState = awaitPhoneState(size = 2)
    assertThat(phoneState.entries.active.screen).isInstanceOf<CharacterDetailScreen>()
    assertThat(phoneState.entries.backwardItems.single().state)
      .isInstanceOf<CharacterListPresenter.State>()

    return requireInstance(phoneState.entries.active.state)
  }

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitPhoneState(
    size: Int
  ): PhoneListDetailPresenter.State {
    while (true) {
      val state = awaitItem()
      if (state is ListDetailPresenter.State.Phone && state.state.entries.count() == size) {
        assertThat(state.state.entries.toList()).hasSize(size)

        return state.state
      }
    }
  }

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitTabletContent(
    selectedCharacterId: String? = null
  ): TabletListDetailPresenter.State.Content {
    while (true) {
      val state = requireInstance<ListDetailPresenter.State.Tablet>(awaitItem()).state
      if (
        state is TabletListDetailPresenter.State.Content &&
          (selectedCharacterId == null ||
            state.listState.selectedCharacterId == selectedCharacterId)
      ) {
        return state
      }
    }
  }

  private inline fun <reified T : Any> requireInstance(value: Any): T {
    assertThat(value).isInstanceOf<T>()

    return value as T
  }

  private fun CharacterListPresenter.State.requireCharacter(characterId: String): Character {
    val character = characters.firstOrNull { it.id == characterId }
    assertThat(character, name = "character $characterId").isNotNull()

    return character as Character
  }

  private fun CharacterListPresenter.State.selectCharacter(characterId: String) {
    requireCharacter(characterId)
    eventSink(CharacterListPresenter.Event.SelectCharacter(characterId))
  }

  private fun CharacterDetailPresenter.State.assertCharacter(
    name: String,
    ageAtRingDestruction: String,
  ) {
    val availableState = requireInstance<CharacterDetailPresenter.DetailState.Available>(state)

    assertThat(availableState.character.name).isEqualTo(name)
    assertThat(availableState.character.ageAtRingDestruction).isEqualTo(ageAtRingDestruction)
  }
}
