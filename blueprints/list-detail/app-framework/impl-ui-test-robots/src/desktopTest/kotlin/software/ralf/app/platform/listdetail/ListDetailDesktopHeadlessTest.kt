package software.ralf.app.platform.listdetail

import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Test
import software.ralf.app.platform.listdetail.presenternavigation.DefaultBackstackModel
import software.ralf.app.platform.listdetail.templates.AppTemplate
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.template.ModelDelegate

/** Desktop integration tests that drive the full application through its template models. */
class ListDetailDesktopHeadlessTest {
  private val headlessTestRule = DesktopHeadlessTestRule()

  @Test
  fun `phone opens character detail and returns to list`() = headlessTestRule.runPhoneTest {
    val listModel = awaitPhoneList()
    listModel.requireCharacter("samwise")
    listModel.selectCharacter("samwise")

    val detailModel = awaitPhoneDetail()
    detailModel.assertCharacter(name = "Samwise Gamgee", ageAtRingDestruction = "38")
    assertThat(detailModel.showBackButton).isTrue()

    detailModel.onBack()

    awaitPhoneList().requireCharacter("samwise")
  }

  @Test
  fun `tablet updates detail without phone navigation`() = headlessTestRule.runTabletTest {
    val initialModel = awaitTabletContent()

    initialModel.listModel.requireCharacter("aragorn")
    assertThat(initialModel.listModel.selectedCharacterId).isEqualTo("frodo")
    initialModel.detailModel.assertCharacter(
      name = "Frodo Baggins",
      ageAtRingDestruction = "50",
    )
    assertThat(initialModel.detailModel.showBackButton).isFalse()

    initialModel.listModel.selectCharacter("aragorn")

    val updatedModel = awaitTabletContent(selectedCharacterId = "aragorn")

    assertThat(updatedModel.listModel.selectedCharacterId).isEqualTo("aragorn")
    updatedModel.detailModel.assertCharacter(name = "Aragorn", ageAtRingDestruction = "88")
    assertThat(updatedModel.detailModel.showBackButton).isFalse()
  }

  private suspend fun ReceiveTurbine<AppTemplate>.awaitPhoneList(): CharacterListPresenter.Model {
    val backstack = awaitPhoneBackstack(size = 1)

    return requireInstance(backstack.backstack.single())
  }

  private suspend fun ReceiveTurbine<AppTemplate>.awaitPhoneDetail():
    CharacterDetailPresenter.Model {
    val backstack = awaitPhoneBackstack(size = 2)

    return requireInstance(backstack.backstack.last())
  }

  private suspend fun ReceiveTurbine<AppTemplate>.awaitPhoneBackstack(
    size: Int
  ): DefaultBackstackModel {
    while (true) {
      val template = requireInstance<AppTemplate.FullScreenTemplate>(awaitItem())
      val backstack = requireInstance<DefaultBackstackModel>(template.model.delegatedModel())

      if (backstack.backstack.size == size) {
        assertThat(backstack.backstack).hasSize(size)

        return backstack
      }
    }
  }

  private suspend fun ReceiveTurbine<AppTemplate>.awaitTabletContent(
    selectedCharacterId: String? = null
  ): TabletListDetailPresenter.Model.Content {
    while (true) {
      val template = requireInstance<AppTemplate.FullScreenTemplate>(awaitItem())
      val model =
        requireInstance<TabletListDetailPresenter.Model.Content>(template.model.delegatedModel())

      if (
        selectedCharacterId == null || model.listModel.selectedCharacterId == selectedCharacterId
      ) {
        return model
      }
    }
  }

  private fun BaseModel.delegatedModel(): BaseModel {
    var model = this

    while (model is ModelDelegate) {
      val delegatedModel = model.delegate()

      if (delegatedModel === model) {
        break
      }

      model = delegatedModel
    }

    return model
  }

  private inline fun <reified T : Any> requireInstance(value: Any): T {
    assertThat(value).isInstanceOf<T>()

    return value as T
  }

  private fun CharacterListPresenter.Model.requireCharacter(characterId: String): Character {
    val character = characters.firstOrNull { it.id == characterId }

    assertThat(character, name = "character $characterId").isNotNull()

    return character as Character
  }

  private fun CharacterListPresenter.Model.selectCharacter(characterId: String) {
    onCharacterSelected(requireCharacter(characterId))
  }

  private fun CharacterDetailPresenter.Model.assertCharacter(
    name: String,
    ageAtRingDestruction: String,
  ) {
    val availableState = requireInstance<CharacterDetailPresenter.State.Available>(state)

    assertThat(availableState.character.name).isEqualTo(name)
    assertThat(availableState.character.ageAtRingDestruction).isEqualTo(ageAtRingDestruction)
  }
}
