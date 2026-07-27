@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail

import androidx.compose.ui.unit.dp
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.listdetail.presenternavigation.DefaultBackstackModel
import software.ralf.app.platform.listdetail.screen.ScreenSize
import software.ralf.app.platform.listdetail.screen.ScreenSizeProvider
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.test
import software.ralf.app.platform.presenter.template.ModelDelegate

class ListDetailPresenterImplTest {
  @Test
  fun `root delegates phone layout to phone presenter`() = runTest {
    presenter(characters()).test(this) {
      val model = awaitItem() as ListDetailPresenterImpl.Model

      assertThat(model.delegate() is PhoneListDetailPresenter.Model).isTrue()
    }
  }

  @Test
  fun `root delegates tablet layout to tablet presenter`() = runTest {
    presenter(
        characters = characters(),
        initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp),
      )
      .test(this) {
        val model = awaitItem() as ListDetailPresenterImpl.Model

        assertThat(model.delegate() is TabletListDetailPresenter.Model).isTrue()
      }
  }

  @Test
  fun `tablet presenter emits updated selection`() = runTest {
    val characters = characters()
    val selectionState = ListDetailSelectionState()
    val repository = FakeCharacterRepository(characters)
    val presenter =
      TabletListDetailPresenter(
        listPresenter = CharacterListPresenter(repository),
        detailPresenterFactory = detailPresenterFactory(repository),
      )

    presenter.test(this, TabletListDetailPresenter.Input(selectionState)) {
      val initialModel = awaitItem()
      initialModel.listModel.onCharacterSelected(characters[1])

      val updatedModel = awaitItem()
      assertThat(updatedModel.listModel.selectedCharacterId).isEqualTo(characters[1].id)
    }
  }

  @Test
  fun `phone selection pushes detail and back returns to list`() = runTest {
    val characters = characters()
    presenter(characters = characters).test(this) {
      val initialBackstack = awaitBackstack(size = 1)
      val listModel = initialBackstack.backstack.single() as CharacterListPresenter.Model

      listModel.onCharacterSelected(characters[1])

      val detailBackstack = awaitBackstack(size = 2)
      val detailModel = detailBackstack.backstack.last() as CharacterDetailPresenter.Model
      assertThat(detailModel.requireCharacter()).isEqualTo(characters[1])
      assertThat(detailModel.showBackButton).isTrue()

      detailBackstack.onBack()

      val poppedBackstack = awaitBackstack(size = 1)
      assertThat(poppedBackstack.backstack.single() is CharacterListPresenter.Model).isTrue()
    }
  }

  @Test
  fun `tablet auto-selects first character and selection replaces detail`() = runTest {
    val characters = characters()
    presenter(
        characters = characters,
        initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp),
      )
      .test(this) {
        val initialModel = awaitTabletContent()
        assertThat(initialModel.listModel.selectedCharacterId).isEqualTo(characters[0].id)
        assertThat(initialModel.detailModel.requireCharacter()).isEqualTo(characters[0])
        assertThat(initialModel.detailModel.showBackButton).isEqualTo(false)

        initialModel.listModel.onCharacterSelected(characters[1])

        val updatedModel = awaitTabletContent(selectedCharacterId = characters[1].id)
        assertThat(updatedModel.detailModel.requireCharacter()).isEqualTo(characters[1])
      }
  }

  @Test
  fun `tablet preserves phone selection after resize`() = runTest {
    val characters = characters()
    val screenSizeProvider = FakeScreenSizeProvider(ScreenSize.Zero)
    presenter(characters = characters, screenSizeProvider = screenSizeProvider).test(this) {
      val phoneModel = awaitBackstack(size = 1)
      val listModel = phoneModel.backstack.single() as CharacterListPresenter.Model
      listModel.onCharacterSelected(characters[1])
      awaitBackstack(size = 2)

      screenSizeProvider.update(ScreenSize.from(width = 800.dp, height = 600.dp))

      val tabletModel = awaitTabletContent(selectedCharacterId = characters[1].id)
      assertThat(tabletModel.detailModel.requireCharacter()).isEqualTo(characters[1])
      assertThat(tabletModel.detailModel.showBackButton).isEqualTo(false)
    }
  }

  @Test
  fun `detail presenter observes repository updates and missing characters`() = runTest {
    val characters = characters()
    val repository = FakeCharacterRepository(characters)
    val presenter =
      CharacterDetailPresenter(
        repository = repository,
        characterId = characters[0].id,
        showBackButton = false,
      )

    presenter.test(this) {
      assertThat(awaitItem().requireCharacter()).isEqualTo(characters[0])

      val updatedCharacter = characters[0].copy(name = "Frodo Gardner")
      repository.update(listOf(updatedCharacter, characters[1]))
      assertThat(awaitItem().requireCharacter()).isEqualTo(updatedCharacter)

      repository.update(listOf(characters[1]))
      assertThat(awaitItem().state)
        .isEqualTo(CharacterDetailPresenter.State.Missing(characters[0].id))
    }
  }

  @Test
  fun `empty tablet has no selection or detail`() = runTest {
    presenter(
        characters = emptyList(),
        initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp),
      )
      .test(this) {
        val model = awaitTabletModel()

        assertThat(model is TabletListDetailPresenter.Model.Empty).isTrue()
        assertThat(model.listModel.characters).hasSize(0)
        assertThat(model.listModel.selectedCharacterId).isNull()
      }
  }

  private fun presenter(
    characters: List<Character>,
    initialScreenSize: ScreenSize = ScreenSize.Zero,
    screenSizeProvider: FakeScreenSizeProvider = FakeScreenSizeProvider(initialScreenSize),
  ): ListDetailPresenter {
    val repository = FakeCharacterRepository(characters)
    val listPresenter = CharacterListPresenter(repository)
    val detailPresenterFactory = detailPresenterFactory(repository)

    return ListDetailPresenterImpl(
      screenSizeProvider = screenSizeProvider,
      phonePresenter =
        PhoneListDetailPresenter(
          listPresenter = listPresenter,
          detailPresenterFactory = detailPresenterFactory,
        ),
      tabletPresenter =
        TabletListDetailPresenter(
          listPresenter = listPresenter,
          detailPresenterFactory = detailPresenterFactory,
        ),
    )
  }

  private fun detailPresenterFactory(
    repository: CharacterRepository
  ): CharacterDetailPresenter.Factory {
    return object : CharacterDetailPresenter.Factory {
      override fun createCharacterDetailPresenter(
        characterId: String,
        showBackButton: Boolean,
      ): CharacterDetailPresenter {
        return CharacterDetailPresenter(
          repository = repository,
          characterId = characterId,
          showBackButton = showBackButton,
        )
      }
    }
  }

  private fun characters(): List<Character> {
    return listOf(
      Character(
        id = "frodo",
        name = "Frodo Baggins",
        ageAtRingDestruction = "50",
        portrait = CharacterPortrait.FRODO,
      ),
      Character(
        id = "samwise",
        name = "Samwise Gamgee",
        ageAtRingDestruction = "38",
        portrait = CharacterPortrait.SAMWISE,
      ),
    )
  }

  private suspend fun ReceiveTurbine<BaseModel>.awaitBackstack(size: Int): DefaultBackstackModel {
    while (true) {
      val model = awaitItem().delegatedModel()
      if (model is DefaultBackstackModel && model.backstack.size == size) {
        return model
      }
    }
  }

  private suspend fun ReceiveTurbine<BaseModel>.awaitTabletModel(
    selectedCharacterId: String? = null
  ): TabletListDetailPresenter.Model {
    while (true) {
      val model = awaitItem().delegatedModel()
      if (
        model is TabletListDetailPresenter.Model &&
          (selectedCharacterId == null ||
            model.listModel.selectedCharacterId == selectedCharacterId)
      ) {
        return model
      }
    }
  }

  private suspend fun ReceiveTurbine<BaseModel>.awaitTabletContent(
    selectedCharacterId: String? = null
  ): TabletListDetailPresenter.Model.Content {
    while (true) {
      val model = awaitItem().delegatedModel()
      if (
        model is TabletListDetailPresenter.Model.Content &&
          (selectedCharacterId == null ||
            model.listModel.selectedCharacterId == selectedCharacterId)
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

  private fun CharacterDetailPresenter.Model.requireCharacter(): Character {
    val state = state
    check(state is CharacterDetailPresenter.State.Available)
    return state.character
  }

  private class FakeScreenSizeProvider(initialScreenSize: ScreenSize) : ScreenSizeProvider {
    private val mutableScreenSize = MutableStateFlow(initialScreenSize)

    override val screenSize: StateFlow<ScreenSize> = mutableScreenSize

    fun update(screenSize: ScreenSize) {
      mutableScreenSize.value = screenSize
    }
  }
}
