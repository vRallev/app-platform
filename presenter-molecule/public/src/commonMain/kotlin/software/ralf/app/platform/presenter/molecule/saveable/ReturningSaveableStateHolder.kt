package software.ralf.app.platform.presenter.molecule.saveable

import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.withCompositionLocals
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import software.ralf.app.platform.ExperimentalAppPlatform

/**
 * Holds saveable state for composable subtrees that return a value.
 *
 * This API serves the same purpose as Compose runtime's [SaveableStateHolder]: wrap a subtree with
 * [SaveableStateProvider], give that subtree a stable `key`, and any state inside the subtree that
 * is created with [rememberSaveable] will be saved when the subtree leaves composition and restored
 * when the same `key` is composed again.
 *
 * The difference from [SaveableStateHolder] is that [SaveableStateProvider] returns the value
 * produced by `content`. This is useful for App Platform presenter code, where composable presenter
 * functions return models rather than rendering `Unit`.
 *
 * Example:
 * ```
 * @OptIn(ExperimentalAppPlatform::class)
 * @Composable
 * fun present(input: Unit): Model {
 *   val stateHolder = rememberReturningSaveableStateHolder()
 *   val presenter = if (showingLogin) loginPresenter else registerPresenter
 *   val presenterKey = if (showingLogin) "login" else "register"
 *
 *   return stateHolder.SaveableStateProvider(key = presenterKey) {
 *     presenter.present(Unit)
 *   }
 * }
 * ```
 *
 * Pick keys that represent the logical subtree whose state should be restored. A key must have
 * stable equality and hash-code behavior, and the same key must not be used by multiple active
 * [SaveableStateProvider] calls at the same time. If the holder is remembered under a parent
 * [LocalSaveableStateRegistry], then keys and values also need to be saveable by that parent
 * registry. On Android this usually means they must be compatible with saved instance state, such
 * as values that can be stored in a `Bundle` or values converted by a custom saver.
 *
 * State is saved in two layers. While this holder is alive, inactive subtree state is kept in
 * memory inside the holder. The holder itself is created with [rememberSaveable], so that in-memory
 * map is offered to the parent [LocalSaveableStateRegistry] when one exists. Without a parent
 * registry, this API still preserves [rememberSaveable] state while switching subtrees in the
 * current composition, but it does not provide persistence across a destroyed composition or
 * process death.
 */
@ExperimentalAppPlatform
public interface ReturningSaveableStateHolder {
  /**
   * Composes [content] under [key] and returns the value produced by [content].
   *
   * All state inside [content] that uses [rememberSaveable] is associated with [key]. When
   * [content] leaves composition, that state is saved. When [content] is later composed with the
   * same [key], the saved values are restored into the subtree.
   *
   * The same [key] cannot be used by more than one active provider at once. Use [removeState] when
   * saved state for a key is no longer useful.
   */
  @Suppress("ComposableNaming")
  @Composable
  public fun <T> SaveableStateProvider(key: Any, content: @Composable () -> T): T

  /** Removes inactive saved state associated with [key]. */
  public fun removeState(key: Any)
}

/**
 * Creates and remembers a [ReturningSaveableStateHolder].
 *
 * This function is equivalent to [rememberSaveableStateHolder], except the returned holder supports
 * value-returning composable content. The holder is remembered with [rememberSaveable], so if a
 * parent [LocalSaveableStateRegistry] is available, the holder can save and restore inactive
 * subtree state through that parent registry.
 */
@ExperimentalAppPlatform
@Composable
public fun rememberReturningSaveableStateHolder(): ReturningSaveableStateHolder =
  rememberSaveable(saver = ReturningSaveableStateHolderImpl.Saver) {
      ReturningSaveableStateHolderImpl()
    }
    .apply { parentSaveableStateRegistry = LocalSaveableStateRegistry.current }

@OptIn(ExperimentalAppPlatform::class)
private class ReturningSaveableStateHolderImpl(
  private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : ReturningSaveableStateHolder {
  private val registries = mutableScatterMapOf<Any, SaveableStateRegistry>()
  var parentSaveableStateRegistry: SaveableStateRegistry? = null
  private val canBeSaved: (Any) -> Boolean = { parentSaveableStateRegistry?.canBeSaved(it) ?: true }

  @Composable
  override fun <T> SaveableStateProvider(key: Any, content: @Composable () -> T): T {
    return returningReusableContent(key) {
      val registry = remember {
        require(canBeSaved(key)) {
          "Type of the key $key is not supported. On Android you can only use types " +
            "which can be stored inside the Bundle."
        }
        ReturningSaveableStateRegistryWrapper(
          base = SaveableStateRegistry(restoredValues = savedStates[key], canBeSaved)
        )
      }
      val model =
        withCompositionLocals(
          LocalSaveableStateRegistry provides registry,
          LocalSavedStateRegistryOwner provides registry,
          content = content,
        )
      DisposableEffect(Unit) {
        require(key !in registries) { "Key $key was used multiple times " }
        savedStates -= key
        registries[key] = registry
        onDispose {
          if (registries.remove(key) === registry) {
            registry.saveTo(savedStates, key)
          }
        }
      }

      model
    }
  }

  private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
    val map = savedStates
    registries.forEach { key, registry -> registry.saveTo(map, key) }
    return map.ifEmpty { null }
  }

  override fun removeState(key: Any) {
    if (registries.remove(key) == null) {
      savedStates -= key
    }
  }

  private fun SaveableStateRegistry.saveTo(
    map: MutableMap<Any, Map<String, List<Any?>>>,
    key: Any,
  ) {
    val savedData = performSave()
    if (savedData.isEmpty()) {
      map -= key
    } else {
      map[key] = savedData
    }
  }

  companion object {
    val Saver: Saver<ReturningSaveableStateHolderImpl, *> =
      Saver(save = { it.saveAll() }, restore = { ReturningSaveableStateHolderImpl(it) })
  }

  @Composable
  private inline fun <T> returningReusableContent(key: Any?, content: @Composable () -> T): T {
    var result: Any? = Unset
    ReusableContent(key) { result = content() }
    @Suppress("UNCHECKED_CAST")
    return result as T
  }
}

/**
 * Local copy of AndroidX runtime-saveable's internal SaveableStateRegistryWrapper.
 *
 * It bridges the child [SaveableStateRegistry] to [SavedStateRegistryOwner] so content below
 * [ReturningSaveableStateHolder.SaveableStateProvider] sees the same saved-state-owner shape that
 * content below Compose's [SaveableStateHolder.SaveableStateProvider] sees.
 */
private class ReturningSaveableStateRegistryWrapper(base: SaveableStateRegistry) :
  SaveableStateRegistry by base, SavedStateRegistryOwner {
  override val lifecycle: LifecycleRegistry
    get() = getOrInitLifecycle()

  private var lifecycleRegistry: LifecycleRegistry? = null

  @Suppress("VisibleForTests")
  private fun getOrInitLifecycle(): LifecycleRegistry {
    return lifecycleRegistry ?: LifecycleRegistry.createUnsafe(this).also { lifecycleRegistry = it }
  }

  private val controller: SavedStateRegistryController
    get() = getOrInitController(savedState = null)

  private var savedStateRegistryController: SavedStateRegistryController? = null

  private fun getOrInitController(savedState: SavedState?): SavedStateRegistryController {
    return savedStateRegistryController
      ?: SavedStateRegistryController.create(owner = this).also {
        savedStateRegistryController = it
        it.performRestore(savedState)
      }
  }

  override val savedStateRegistry
    get() = controller.savedStateRegistry

  init {
    val savedState = consumeRestored(key = SAVED_STATE_REGISTRY_PROVIDER_KEY) as? SavedState
    if (savedState != null) {
      getOrInitController(savedState)
    }

    registerProvider(key = SAVED_STATE_REGISTRY_PROVIDER_KEY) {
      val controller = savedStateRegistryController
      if (controller != null) {
        val result = savedState()
        controller.performSave(outBundle = result)
        if (result.read { isEmpty() }) null else result
      } else {
        null
      }
    }
  }
}

private const val SAVED_STATE_REGISTRY_PROVIDER_KEY = "androidx.savedstate.SavedStateRegistry"

private object Unset
