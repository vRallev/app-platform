package software.ralf.circuit.listdetail

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.circuit.listdetail.screen.ScreenSize
import software.ralf.circuit.listdetail.screen.ScreenSizeProvider

class ListDetailPresenterTest {
  @Test
  fun `root delegates phone layout to phone presenter`() = runTest {
    presenter().test {
      assertThat(awaitItem()).isInstanceOf<ListDetailPresenter.State.Phone>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `root delegates tablet layout to tablet presenter`() = runTest {
    presenter(initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp)).test {
      assertThat(awaitItem()).isInstanceOf<ListDetailPresenter.State.Tablet>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `tablet presenter emits updated selection`() = runTest {
    val characters = characters()
    val repository = FakeCharacterRepository(characters)
    tabletPresenter(repository).test {
      awaitItem().listState.selectCharacter(characters[1].id)

      val updatedState = awaitState { it.listState.selectedCharacterId == characters[1].id }
      assertThat(updatedState.listState.selectedCharacterId).isEqualTo(characters[1].id)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `phone selection pushes detail and back returns to list`() = runTest {
    val characters = characters()
    presenter(repository = FakeCharacterRepository(characters)).test {
      val initialState = awaitPhoneState(size = 1)
      val listState = initialState.entries.active.state as CharacterListPresenter.State
      listState.selectCharacter(characters[1].id)

      val detailState = awaitPhoneState(size = 2)
      val characterDetailState = detailState.entries.active.state as CharacterDetailPresenter.State
      assertThat(detailState.entries.active.screen)
        .isEqualTo(CharacterDetailScreen(characters[1].id))
      assertThat(characterDetailState.requireCharacter()).isEqualTo(characters[1])
      assertThat(characterDetailState.showBackButton).isTrue()

      detailState.eventSink(PhoneListDetailPresenter.Event.Back)

      val poppedState = awaitPhoneState(size = 1)
      assertThat(poppedState.entries.active.state).isInstanceOf<CharacterListPresenter.State>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `tablet auto-selects first character and selection replaces detail`() = runTest {
    val characters = characters()
    presenter(
        repository = FakeCharacterRepository(characters),
        initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp),
      )
      .test {
        val initialState = awaitTabletContent()
        assertThat(initialState.listState.selectedCharacterId).isEqualTo(characters[0].id)
        assertThat(initialState.detailState.requireCharacter()).isEqualTo(characters[0])
        assertThat(initialState.detailState.showBackButton).isFalse()

        initialState.listState.selectCharacter(characters[1].id)

        val updatedState = awaitTabletContent(selectedCharacterId = characters[1].id)
        assertThat(updatedState.detailState.requireCharacter()).isEqualTo(characters[1])
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `tablet preserves phone selection after resize`() = runTest {
    val characters = characters()
    val screenSizeProvider = FakeScreenSizeProvider(ScreenSize.Zero)
    presenter(
        repository = FakeCharacterRepository(characters),
        screenSizeProvider = screenSizeProvider,
      )
      .test {
        val listState =
          awaitPhoneState(size = 1).entries.active.state as CharacterListPresenter.State
        listState.selectCharacter(characters[1].id)
        awaitPhoneState(size = 2)

        screenSizeProvider.update(ScreenSize.from(width = 800.dp, height = 600.dp))

        val tabletState = awaitTabletContent(selectedCharacterId = characters[1].id)
        assertThat(tabletState.detailState.requireCharacter()).isEqualTo(characters[1])
        assertThat(tabletState.detailState.showBackButton).isFalse()
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `detail presenter observes repository updates and missing characters`() = runTest {
    val characters = characters()
    val repository = FakeCharacterRepository(characters)
    val presenter =
      CharacterDetailPresenter(
        repository = repository,
        screen = CharacterDetailScreen(characters[0].id, showBackButton = false),
        navigator = Navigator.NoOp,
      )

    presenter.test {
      assertThat(awaitItem().requireCharacter()).isEqualTo(characters[0])

      val updatedCharacter = characters[0].copy(name = "Frodo Gardner")
      repository.update(listOf(updatedCharacter, characters[1]))
      assertThat(awaitItem().requireCharacter()).isEqualTo(updatedCharacter)

      repository.update(listOf(characters[1]))
      assertThat(awaitItem().state)
        .isEqualTo(CharacterDetailPresenter.DetailState.Missing(characters[0].id))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `empty tablet has no selection or detail`() = runTest {
    presenter(
        repository = FakeCharacterRepository(),
        initialScreenSize = ScreenSize.from(width = 800.dp, height = 600.dp),
      )
      .test {
        val state = (awaitItem() as ListDetailPresenter.State.Tablet).state

        assertThat(state).isInstanceOf<TabletListDetailPresenter.State.Empty>()
        assertThat(state.listState.characters).hasSize(0)
        assertThat(state.listState.selectedCharacterId).isNull()
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `returning to phone resets navigation and keeps tablet selection`() = runTest {
    val characters = characters()
    val screenSizeProvider = FakeScreenSizeProvider(ScreenSize.Zero)
    presenter(
        repository = FakeCharacterRepository(characters),
        screenSizeProvider = screenSizeProvider,
      )
      .test {
        val phoneList =
          awaitPhoneState(size = 1).entries.active.state as CharacterListPresenter.State
        phoneList.selectCharacter(characters[1].id)
        awaitPhoneState(size = 2)

        screenSizeProvider.update(ScreenSize.from(width = 800.dp, height = 600.dp))
        awaitTabletContent(selectedCharacterId = characters[1].id)
          .listState
          .selectCharacter(characters[0].id)
        awaitTabletContent(selectedCharacterId = characters[0].id)

        screenSizeProvider.update(ScreenSize.from(width = 480.dp, height = 840.dp))

        val phoneState = awaitPhoneState(size = 1)
        val restoredList = phoneState.entries.active.state as CharacterListPresenter.State
        assertThat(phoneState.entries.toList()).hasSize(1)
        assertThat(restoredList.selectedCharacterId).isEqualTo(characters[0].id)
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun `tablet repairs invalid selection and falls back when selected character is removed`() =
    runTest {
      val characters = characters()
      val repository = FakeCharacterRepository(characters)
      val selectionState = ListDetailSelectionState(selectedCharacterId = "unavailable")
      tabletPresenter(repository, selectionState).test {
        val initialState = awaitItem() as TabletListDetailPresenter.State.Content
        assertThat(initialState.listState.selectedCharacterId).isEqualTo(characters[0].id)
        assertThat(initialState.detailState.requireCharacter()).isEqualTo(characters[0])

        initialState.listState.selectCharacter(characters[1].id)
        awaitState { it.listState.selectedCharacterId == characters[1].id }

        repository.update(listOf(characters[0]))

        val fallbackState =
          awaitState { it.listState.selectedCharacterId == characters[0].id }
            as TabletListDetailPresenter.State.Content
        assertThat(fallbackState.listState.characters).isEqualTo(listOf(characters[0]))
        assertThat(fallbackState.detailState.requireCharacter()).isEqualTo(characters[0])

        repository.update(emptyList())

        val emptyState = awaitState { it is TabletListDetailPresenter.State.Empty }
        assertThat(emptyState.listState.characters).hasSize(0)
        assertThat(emptyState.listState.selectedCharacterId).isNull()
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `new feature composition starts with no selection or phone navigation history`() = runTest {
    val presenter = presenter()
    presenter.test {
      val listState = awaitPhoneState(size = 1).entries.active.state as CharacterListPresenter.State
      listState.selectCharacter("samwise")
      awaitPhoneState(size = 2)
      cancelAndIgnoreRemainingEvents()
    }

    presenter.test {
      val phoneState = awaitPhoneState(size = 1)
      val listState = phoneState.entries.active.state as CharacterListPresenter.State
      assertThat(phoneState.entries.toList()).hasSize(1)
      assertThat(listState.selectedCharacterId).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenter(
    repository: CharacterRepository = FakeCharacterRepository(characters()),
    initialScreenSize: ScreenSize = ScreenSize.Zero,
    screenSizeProvider: FakeScreenSizeProvider = FakeScreenSizeProvider(initialScreenSize),
  ): ListDetailPresenter {
    return ListDetailPresenter(
      screenSizeProvider = screenSizeProvider,
      phonePresenterFactory =
        PhoneListDetailPresenter.Factory { selectionState ->
          PhoneListDetailPresenter(
            listPresenterFactory = listPresenterFactory(repository),
            detailPresenterFactory = detailPresenterFactory(repository),
            circuitSaver = CircuitSaver.NoOp,
            selectionState = selectionState,
          )
        },
      tabletPresenterFactory =
        TabletListDetailPresenter.Factory { selectionState ->
          tabletPresenter(repository, selectionState)
        },
    )
  }

  private fun tabletPresenter(
    repository: CharacterRepository,
    selectionState: ListDetailSelectionState = ListDetailSelectionState(),
  ): TabletListDetailPresenter {
    return TabletListDetailPresenter(
      listPresenterFactory = listPresenterFactory(repository),
      detailPresenterFactory = detailPresenterFactory(repository),
      selectionState = selectionState,
    )
  }

  private fun listPresenterFactory(
    repository: CharacterRepository
  ): CharacterListPresenter.Factory {
    return CharacterListPresenter.Factory { screen, navigator ->
      CharacterListPresenter(repository, screen, navigator)
    }
  }

  private fun detailPresenterFactory(
    repository: CharacterRepository
  ): CharacterDetailPresenter.Factory {
    return CharacterDetailPresenter.Factory { screen, navigator ->
      CharacterDetailPresenter(repository, screen, navigator)
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

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitPhoneState(
    size: Int
  ): PhoneListDetailPresenter.State {
    val state = awaitState {
      it is ListDetailPresenter.State.Phone && it.state.entries.count() == size
    }
    return (state as ListDetailPresenter.State.Phone).state
  }

  private suspend fun CircuitReceiveTurbine<ListDetailPresenter.State>.awaitTabletContent(
    selectedCharacterId: String? = null
  ): TabletListDetailPresenter.State.Content {
    val state = awaitState {
      it is ListDetailPresenter.State.Tablet &&
        it.state is TabletListDetailPresenter.State.Content &&
        (selectedCharacterId == null ||
          it.state.listState.selectedCharacterId == selectedCharacterId)
    }
    return (state as ListDetailPresenter.State.Tablet).state
      as TabletListDetailPresenter.State.Content
  }

  private suspend fun <T : CircuitUiState> CircuitReceiveTurbine<T>.awaitState(
    predicate: (T) -> Boolean
  ): T {
    while (true) {
      val state = awaitItem()
      if (predicate(state)) {
        return state
      }
    }
  }

  private fun CharacterListPresenter.State.selectCharacter(characterId: String) {
    eventSink(CharacterListPresenter.Event.SelectCharacter(characterId))
  }

  private fun CharacterDetailPresenter.State.requireCharacter(): Character {
    return (state as CharacterDetailPresenter.DetailState.Available).character
  }

  private class FakeScreenSizeProvider(initialScreenSize: ScreenSize) : ScreenSizeProvider {
    private val mutableScreenSize = MutableStateFlow(initialScreenSize)

    override val screenSize: StateFlow<ScreenSize> = mutableScreenSize

    fun update(screenSize: ScreenSize) {
      mutableScreenSize.value = screenSize
    }
  }
}
