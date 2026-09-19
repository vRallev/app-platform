package software.ralf.app.platform.presenter.compose.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.test

@OptIn(ExperimentalAppPlatform::class)
class ReturningSaveableStateHolderTest {

  @Test
  fun `restores rememberSaveable state when a key returns to composition`() = runTest {
    val key = MutableStateFlow("first")

    SaveablePresenter(key).test(this) {
      val firstInitial = awaitItem()
      assertThat(firstInitial.key).isEqualTo("first")
      assertThat(firstInitial.count).isEqualTo(0)

      firstInitial.increment()
      assertThat(awaitItem().count).isEqualTo(1)

      key.value = "second"
      val secondInitial = awaitItem()
      assertThat(secondInitial.key).isEqualTo("second")
      assertThat(secondInitial.count).isEqualTo(0)

      key.value = "first"
      val firstRestored = awaitItem()
      assertThat(firstRestored.key).isEqualTo("first")
      assertThat(firstRestored.count).isEqualTo(1)
    }
  }

  @Test
  fun `removeState drops inactive state for a key`() = runTest {
    val key = MutableStateFlow("first")

    SaveablePresenter(key).test(this) {
      val firstInitial = awaitItem()
      firstInitial.increment()
      val firstUpdated = awaitItem()
      assertThat(firstUpdated.count).isEqualTo(1)

      firstUpdated.removeState()

      key.value = "second"
      assertThat(awaitItem().key).isEqualTo("second")

      key.value = "first"
      val firstReset = awaitItem()
      assertThat(firstReset.key).isEqualTo("first")
      assertThat(firstReset.count).isEqualTo(0)
    }
  }

  @Test
  fun `provides a saved state registry owner to content`() = runTest {
    SaveablePresenter(MutableStateFlow("first")).test(this) {
      assertThat(awaitItem().hasSavedStateRegistryOwner).isTrue()
    }
  }

  @Test
  fun `supports non BaseModel return types from content`() = runTest {
    PlainReturnPresenter().test(this) {
      assertThat(awaitItem().plainValue).isEqualTo(PlainValue("plain"))
    }
  }

  @Test
  fun `saves holder state through a parent saveable registry`() = runTest {
    val firstParentRegistry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
    var savedValues: Map<String, List<Any?>>? = null

    SaveablePresenter(key = MutableStateFlow("first"), parentRegistry = firstParentRegistry).test(
      this
    ) {
      val firstInitial = awaitItem()
      firstInitial.increment()
      assertThat(awaitItem().count).isEqualTo(1)

      savedValues = firstParentRegistry.performSave()
    }

    val restoredParentRegistry =
      SaveableStateRegistry(restoredValues = savedValues, canBeSaved = { true })

    SaveablePresenter(key = MutableStateFlow("first"), parentRegistry = restoredParentRegistry)
      .test(this) {
        val restored = awaitItem()
        assertThat(restored.key).isEqualTo("first")
        assertThat(restored.count).isEqualTo(1)
      }
  }

  @Test
  fun `throws when the parent registry cannot save the key`() = runTest {
    val parentRegistry =
      SaveableStateRegistry(restoredValues = null, canBeSaved = { it is String || it is Int })

    try {
      NonSaveableKeyPresenter(parentRegistry).test(this) { awaitItem() }
      fail("Expected the holder to reject a non-saveable key.")
    } catch (_: IllegalArgumentException) {}
  }

  private class SaveablePresenter(
    private val key: MutableStateFlow<String>,
    private val parentRegistry: SaveableStateRegistry? = null,
  ) : ComposePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      return if (parentRegistry != null) {
        withCompositionLocal(LocalSaveableStateRegistry provides parentRegistry) {
          presentSaveableContent()
        }
      } else {
        presentSaveableContent()
      }
    }

    @Composable
    private fun presentSaveableContent(): Model {
      val selectedKey by key.collectAsState()
      val stateHolder = rememberReturningSaveableStateHolder()

      return stateHolder.SaveableStateProvider(key = selectedKey) {
        var count by rememberSaveable { mutableIntStateOf(0) }
        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
        savedStateRegistryOwner.savedStateRegistry

        Model(
          key = selectedKey,
          count = count,
          hasSavedStateRegistryOwner = true,
          increment = { count += 1 },
          removeState = { stateHolder.removeState(selectedKey) },
        )
      }
    }
  }

  private class NonSaveableKeyPresenter(private val parentRegistry: SaveableStateRegistry) :
    ComposePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      return withCompositionLocal(LocalSaveableStateRegistry provides parentRegistry) {
        val stateHolder = rememberReturningSaveableStateHolder()
        stateHolder.SaveableStateProvider(key = NonSaveableKey) {
          Model(
            key = "non-saveable",
            count = 0,
            hasSavedStateRegistryOwner = true,
            increment = {},
            removeState = {},
          )
        }
      }
    }
  }

  private data class Model(
    val key: String,
    val count: Int,
    val hasSavedStateRegistryOwner: Boolean,
    val increment: () -> Unit,
    val removeState: () -> Unit,
  ) : BaseModel

  private class PlainReturnPresenter : ComposePresenter<Unit, PlainReturnModel> {
    @Composable
    override fun present(input: Unit): PlainReturnModel {
      val stateHolder = rememberReturningSaveableStateHolder()
      val plainValue =
        stateHolder.SaveableStateProvider(key = "plain") { PlainValue(value = "plain") }

      return PlainReturnModel(plainValue)
    }
  }

  private data class PlainReturnModel(val plainValue: PlainValue) : BaseModel

  private data class PlainValue(val value: String)

  private data object NonSaveableKey
}
