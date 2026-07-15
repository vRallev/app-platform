package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.ComposeRenderer

/**
 * Base Navigation 3 renderer for presenter-owned backstacks.
 *
 * Consumers are expected to define their own [PresenterBackstackModel] type and contribute their
 * own renderer for that model. This base class owns the generic Navigation 3 integration: it
 * creates stable `NavDisplay` keys for entries in [PresenterBackstackModel.backstack], retains
 * popped models while Navigation 3 finishes exit transitions, and forwards
 * [PresenterBackstackModel.onBack] to Navigation 3.
 *
 * Implement [ComposeBackstackEntry] to render each model in the backstack:
 * ```kotlin
 * data class MyBackstackModel(
 *   override val backstack: List<BaseModel>,
 *   override val onBack: () -> Unit,
 * ) : PresenterBackstackModel
 *
 * @ContributesRenderer(MyBackstackModel::class)
 * class MyBackstackRenderer(
 *   private val rendererFactory: RendererFactory,
 * ) : PresenterBackstackRenderer<MyBackstackModel>() {
 *   @Composable
 *   override fun ComposeBackstackEntry(model: BaseModel) {
 *     rendererFactory.getComposeRenderer(model).renderCompose(model)
 *   }
 * }
 * ```
 *
 * Override [PresenterNavDisplay] when a renderer needs custom Navigation 3 behavior, such as
 * app-specific transitions. The override must pass the provided [backstack][PresenterNavDisplay],
 * [onBack][PresenterNavDisplay], [entryProvider][PresenterNavDisplay], and
 * [modifier][PresenterNavDisplay] parameters to `NavDisplay`; those values connect the
 * presenter-owned stack to Navigation 3 and keep retained entries renderable during transitions.
 *
 * ```kotlin
 * @Composable
 * override fun PresenterNavDisplay(
 *   backstack: List<Int>,
 *   onBack: () -> Unit,
 *   entryProvider: (Int) -> NavEntry<Int>,
 *   modifier: Modifier,
 * ) {
 *   NavDisplay(
 *     backStack = backstack,
 *     onBack = onBack,
 *     entryProvider = entryProvider,
 *     modifier = modifier,
 *     transitionSpec = { myPushTransition() },
 *     popTransitionSpec = { myPopTransition() },
 *     predictivePopTransitionSpec = { myPopTransition() },
 *   )
 * }
 * ```
 */
@ExperimentalAppPlatform
public abstract class PresenterBackstackRenderer<in ModelT : PresenterBackstackModel> :
  ComposeRenderer<ModelT>() {
  @Composable
  final override fun Compose(model: ModelT, modifier: Modifier) {
    val entryStore = remember { PresenterBackstackEntryStore<BaseModel>() }
    val backstack = entryStore.sync(model.backstack)

    val entryProvider: (Int) -> NavEntry<Int> = { key ->
      NavEntry(key) { entryKey ->
        DisposableEffect(entryKey) {
          entryStore.retain(entryKey)
          onDispose { entryStore.release(entryKey) }
        }

        ComposeBackstackEntry(entryStore.entryFor(entryKey))
      }
    }

    PresenterNavDisplay(backstack, model.onBack, entryProvider, modifier)
  }

  /**
   * Renders the Navigation 3 `NavDisplay` for the presenter-owned backstack.
   *
   * Override this function to customize Navigation 3 behavior, most commonly by adding transition
   * specs or other `NavDisplay` options. Overrides must apply all parameters to `NavDisplay`:
   * [backstack] is the stable key stack managed by this renderer, [onBack] delegates back gestures
   * to the presenter model, [entryProvider] renders active and transition-retained entries, and
   * [modifier] carries the modifier passed to [Compose].
   */
  @Composable
  public open fun PresenterNavDisplay(
    backstack: List<Int>,
    onBack: () -> Unit,
    entryProvider: (Int) -> NavEntry<Int>,
    modifier: Modifier,
  ) {
    NavDisplay(
      backStack = backstack,
      onBack = onBack,
      entryProvider = entryProvider,
      modifier = modifier,
    )
  }

  /**
   * Renders one model from [PresenterBackstackModel.backstack] inside its Navigation 3 entry.
   *
   * This is called for the active destination and for any popped destination that Navigation 3
   * keeps composed while an exit transition finishes. A typical implementation delegates to
   * `RendererFactory`:
   * ```kotlin
   * @Composable
   * override fun ComposeBackstackEntry(model: BaseModel) {
   *   rendererFactory.getComposeRenderer(model).renderCompose(model)
   * }
   * ```
   */
  @Composable public abstract fun ComposeBackstackEntry(model: BaseModel)

  /**
   * Assigns stable Navigation 3 keys to presenter backstack entries and retains popped entries long
   * enough for exit transitions to finish.
   */
  internal class PresenterBackstackEntryStore<T> {
    private val activeKeys = mutableListOf<Int>()
    private val composedKeyCounts = mutableMapOf<Int, Int>()
    private val retainedEntries = mutableStateMapOf<Int, T>()
    private var nextKey = 0

    /** Reconciles the store with [backstack] and returns active navigation keys in stack order. */
    fun sync(backstack: List<T>): List<Int> {
      while (activeKeys.size < backstack.size) {
        activeKeys += nextKey++
      }
      while (activeKeys.size > backstack.size) {
        val removedKey = activeKeys.removeAt(activeKeys.lastIndex)
        removeIfInactiveAndUncomposed(removedKey)
      }

      backstack.forEachIndexed { index, entry -> retainedEntries[activeKeys[index]] = entry }

      return activeKeys.toList()
    }

    /** Marks [key]'s NavEntry content as currently composed. */
    fun retain(key: Int) {
      composedKeyCounts[key] = composedKeyCounts.getOrElse(key) { 0 } + 1
    }

    /** Returns the active or transition-retained entry for [key]. */
    fun entryFor(key: Int): T {
      return checkNotNull(retainedEntries[key]) {
        "No retained presenter backstack entry for key $key."
      }
    }

    /** Releases [key] after its NavEntry content leaves composition. */
    fun release(key: Int) {
      val composedCount = composedKeyCounts.getOrElse(key) { 0 }
      if (composedCount <= 1) {
        composedKeyCounts.remove(key)
      } else {
        composedKeyCounts[key] = composedCount - 1
      }

      removeIfInactiveAndUncomposed(key)
    }

    private fun removeIfInactiveAndUncomposed(key: Int) {
      if (key !in activeKeys && key !in composedKeyCounts) {
        retainedEntries.remove(key)
      }
    }
  }
}
