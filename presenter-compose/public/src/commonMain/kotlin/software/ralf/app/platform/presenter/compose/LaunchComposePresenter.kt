package software.ralf.app.platform.presenter.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withCompositionLocal
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.Presenter

internal val LocalRecompositionMode: ProvidableCompositionLocal<RecompositionMode> =
  compositionLocalOf {
    error("No RecompositionMode is available in the current presenter hierarchy.")
  }

/**
 * Launch a coroutine into this [CoroutineScope] which will continually recompose
 * [ComposePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> CoroutineScope.launchComposePresenter(
  presenter: ComposePresenter<InputT, ModelT>,
  input: StateFlow<InputT>,
  recompositionMode: RecompositionMode,
): Presenter<ModelT> {
  return object : Presenter<ModelT> {
    override val model: StateFlow<ModelT> =
      launchMolecule(recompositionMode) {
        withCompositionLocal(LocalRecompositionMode provides recompositionMode) {
          val inputElement by input.collectAsState()
          presenter.present(inputElement)
        }
      }
  }
}

/**
 * Launch a coroutine into this [ComposePresenterScope] which will continually recompose
 * [ComposePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> ComposePresenterScope.launchComposePresenter(
  presenter: ComposePresenter<InputT, ModelT>,
  input: StateFlow<InputT>,
): Presenter<ModelT> =
  coroutineScope.launchComposePresenter(
    presenter = presenter,
    input = input,
    recompositionMode = recompositionMode,
  )

/**
 * Launch a coroutine into this [ComposePresenterScope] which will continually recompose
 * [ComposePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> ComposePresenterScope.launchComposePresenter(
  presenter: ComposePresenter<InputT, ModelT>,
  input: InputT,
): Presenter<ModelT> =
  launchComposePresenter(presenter = presenter, input = MutableStateFlow(input))

/**
 * Presents this [ComposePresenter] in a detached Molecule composition and returns the latest
 * [ModelT].
 *
 * Calling [ComposePresenter.present] directly composes a child presenter inline with its parent.
 * That keeps the hierarchy simple, but every parent recomposition also invokes every inline child
 * presenter below it. [presentDetached] creates a separate presenter hierarchy instead. Parent
 * recompositions keep collecting the detached model, but the detached presenter only recomposes
 * when its own [input] or its own state changes.
 *
 * Use this for presenter subtrees that are expensive to compute and whose input changes less often
 * than the parent presenter. Prefer a direct [ComposePresenter.present] call for cheap presenters
 * or presenters that need to participate in the parent's composition locals beyond the App Platform
 * locals that [presentDetached] explicitly preserves.
 *
 * By default, this function uses the [RecompositionMode] from the current presenter hierarchy. Pass
 * [recompositionMode] explicitly if this is called from a composition that was not created through
 * [launchComposePresenter].
 *
 * When [input] changes, the parent presenter can emit one model with the new parent input and the
 * previous detached child model before the detached hierarchy catches up. Use a direct
 * [ComposePresenter.present] call instead if parent and child model state must be updated
 * atomically in the same emission. This is not a concern when the detached presenter always
 * receives the same input, such as [Unit]; in that case, child presenter updates are driven by the
 * detached hierarchy's own state changes.
 */
@Composable
@ExperimentalAppPlatform
public fun <InputT : Any, ModelT : BaseModel> ComposePresenter<InputT, ModelT>.presentDetached(
  input: InputT,
  recompositionMode: RecompositionMode = LocalRecompositionMode.current,
): ModelT {
  val parentCoroutineScope = rememberCoroutineScope()
  val presenter = this
  val detachedObserver =
    remember(presenter, recompositionMode) {
      val inputFlow = MutableStateFlow(input)
      // launchComposePresenter() returns a Presenter with a StateFlow, but not the Job running
      // the composition. Use a child scope so this detached hierarchy can be canceled when it
      // leaves composition, without canceling the parent presenter scope.
      val detachedCoroutineScope = parentCoroutineScope.createChildScope()
      DetachedObserver(
        coroutineScope = detachedCoroutineScope,
        input = inputFlow,
        model =
          detachedCoroutineScope
            .launchComposePresenter(
              presenter = presenter,
              input = inputFlow,
              recompositionMode = recompositionMode,
            )
            .model,
      )
    }

  SideEffect { detachedObserver.input.value = input }

  return detachedObserver.model.collectAsState().value
}

private class DetachedObserver<InputT : Any, ModelT : BaseModel>(
  private val coroutineScope: CoroutineScope,
  val input: MutableStateFlow<InputT>,
  val model: StateFlow<ModelT>,
) : RememberObserver {
  override fun onRemembered() = Unit

  override fun onForgotten() {
    cancel()
  }

  override fun onAbandoned() {
    cancel()
  }

  private fun cancel() {
    coroutineScope.cancel()
  }
}

/**
 * Creates a scope that is linked to this parent scope but can also be canceled on its own.
 *
 * This lets the detached Molecule composition stop when [DetachedObserver] is forgotten or
 * abandoned. If the parent composition is canceled first, structured cancellation still cancels the
 * detached composition through the parent [Job].
 */
private fun CoroutineScope.createChildScope(): CoroutineScope {
  return CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
}
