@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withCompositionLocal
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.saveable.ReturningSaveableStateHolder
import software.ralf.app.platform.presenter.compose.saveable.rememberReturningSaveableStateHolder

/**
 * Receiver scope for [presenterBackstack]. [lastBackstackChange] observes the current stack and
 * [push], [pop], and [replaceTop] mutate the presenter-owned navigation state.
 */
@ExperimentalAppPlatform
public interface PresenterBackstackScope {

  /** Provides the last change made to the backstack. */
  public val lastBackstackChange: State<BackstackChange>

  /** Pushes a new presenter to the top of the backstack. */
  public fun push(presenter: ComposePresenter<Unit, out BaseModel>)

  /**
   * Removes the top presenter from the backstack.
   *
   * The stack always contains the initial presenter provided to [presenterBackstack], so popping
   * the root presenter is ignored.
   */
  public fun pop()

  /** Replaces the top presenter in the stack with [presenter]. */
  public fun replaceTop(presenter: ComposePresenter<Unit, out BaseModel>)

  /** Describes the current state of the backstack and the last operation applied to it. */
  public interface BackstackChange {

    /** The current presenter stack. This list always contains at least the initial presenter. */
    public val backstack: List<ComposePresenter<Unit, out BaseModel>>

    /** The last action applied to the backstack. */
    public val action: Action

    /** The actions that can be applied to the backstack. */
    public enum class Action {
      /** A new presenter was added to the top of the stack. */
      PUSH,

      /** The top presenter was removed from the stack. */
      POP,

      /** The top presenter was replaced. */
      REPLACE,
    }
  }
}

/** Convenience accessor for the current presenter stack. */
@ExperimentalAppPlatform
public val PresenterBackstackScope.backstack: List<ComposePresenter<Unit, out BaseModel>>
  get() = lastBackstackChange.value.backstack

/**
 * Provides the closest presenter backstack scope, or `null` when no presenter backstack is active.
 */
@ExperimentalAppPlatform
public val LocalBackstackScope: ProvidableCompositionLocal<PresenterBackstackScope?> =
  compositionLocalOf {
    null
  }

/** Returns the current [PresenterBackstackScope], or throws if none is available. */
@ExperimentalAppPlatform
@Composable
public fun CompositionLocal<PresenterBackstackScope?>.requireNotNull(): PresenterBackstackScope {
  return checkNotNull(current) {
    "Couldn't find the PresenterBackstackScope in the presenter hierarchy."
  }
}

/**
 * Creates a new backstack for presenters.
 *
 * [initialPresenter] is always kept as the root entry. [content] receives the models produced by
 * every presenter in the stack and returns the model for the presenter calling
 * [presenterBackstack]. Child presenters can access this scope through [LocalBackstackScope].
 *
 * A presenter usually wraps the computed model stack in an app-specific [PresenterBackstackModel],
 * then embeds that backstack model in a larger screen model. In this example, [presenterBackstack]
 * calls `tutorialPresenter.present(Unit)` before invoking [content], so the `backstack` argument
 * already contains the tutorial model before `WelcomePresenter.Model` is created.
 *
 * See [PresenterBackstackRenderer] for how to render `MyBackstackModel` from the sample below.
 *
 * ```kotlin
 * data class MyBackstackModel(
 *   override val backstack: List<BaseModel>,
 *   override val onBack: () -> Unit,
 * ) : PresenterBackstackModel
 *
 * class WelcomePresenter(
 *   private val tutorialPresenter: TutorialPresenter,
 * ) : ComposePresenter<Unit, WelcomePresenter.Model> {
 *   @Composable
 *   override fun present(input: Unit): Model {
 *     return presenterBackstack(tutorialPresenter) { backstack ->
 *       // backstack contains already the model from tutorialPresenter on the first call.
 *
 *       // Notice that WelcomePresenter's own model is returned, which is wrapping the specific
 *       // backstack model
 *       Model(
 *         contentModel =
 *           MyBackstackModel(
 *             backstack = backstack,
 *             onBack = { pop() },
 *           ),
 *       )
 *     }
 *   }
 *
 *   data class Model(val contentModel: MyBackstackModel) : BaseModel
 * }
 *
 * class WelcomeRenderer(
 *   private val rendererFactory: RendererFactory,
 * ) : ComposeRenderer<WelcomePresenter.Model>() {
 *   @Composable
 *   override fun Compose(model: WelcomePresenter.Model, modifier: Modifier) {
 *     rendererFactory
 *       .getComposeRenderer(model.contentModel)
 *       .renderCompose(model.contentModel, modifier)
 *   }
 * }
 * ```
 *
 * A child presenter can get the nearest backstack scope from [LocalBackstackScope]. For example,
 * the initial `TutorialPresenter` can push `SignInPresenter` into the stack:
 * ```kotlin
 * class TutorialPresenter(
 *   private val signInPresenter: SignInPresenter,
 * ) : ComposePresenter<Unit, TutorialModel> {
 *   @Composable
 *   override fun present(input: Unit): TutorialModel {
 *     val backstack = LocalBackstackScope.requireNotNull()
 *
 *     return TutorialModel(
 *       onContinue = { backstack.push(signInPresenter) },
 *       onBack = { backstack.pop() },
 *     )
 *   }
 * }
 * ```
 *
 * Each presenter entry is wrapped in a `SaveableStateProvider` backed by
 * [ReturningSaveableStateHolder]. This gives every entry its own saveable state bucket, so
 * `rememberSaveable { }` state inside child presenters is isolated per backstack entry, including
 * when the same presenter instance is pushed more than once. When an entry is removed from the
 * backstack, this function removes the saveable state associated with that entry.
 */
@ExperimentalAppPlatform
@Composable
public fun <ModelT : BaseModel> presenterBackstack(
  initialPresenter: ComposePresenter<Unit, out BaseModel>,
  content: @Composable PresenterBackstackScope.(List<BaseModel>) -> ModelT,
): ModelT {
  val scope = remember { PresenterBackstackScopeImpl(initialPresenter) }
  val saveableStateHolder = rememberReturningSaveableStateHolder()
  val stateCleaner = remember { RemovedEntryStateCleaner(saveableStateHolder) }

  return withCompositionLocal(LocalBackstackScope provides scope) {
    val presenterBackstack = scope.presenterBackstackEntries
    SideEffect { stateCleaner.removeStateForDroppedEntries(presenterBackstack) }

    val modelBackstack = presenterBackstack.map { entry ->
      saveableStateHolder.SaveableStateProvider(key = entry.key) { entry.presenter.present(Unit) }
    }

    content.invoke(scope, modelBackstack)
  }
}

private class RemovedEntryStateCleaner(private val stateHolder: ReturningSaveableStateHolder) {
  private var previousEntries = emptyList<PresenterBackstackEntry>()

  fun removeStateForDroppedEntries(currentEntries: List<PresenterBackstackEntry>) {
    val currentKeys = currentEntries.mapTo(mutableSetOf()) { it.key }
    previousEntries
      .filter { entry -> entry.key !in currentKeys }
      .forEach { entry -> stateHolder.removeState(entry.key) }
    previousEntries = currentEntries
  }
}
