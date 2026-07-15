package software.ralf.app.platform.presenter.molecule.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.returningCompositionLocalProvider

private class TestBackGestureDispatcherPresenter<InputT : Any, ModelT : BaseModel>(
  private val delegate: MoleculePresenter<InputT, ModelT>,
  private val backEvents: Flow<Flow<BackEventPresenter>>,
) : MoleculePresenter<InputT, ModelT> {
  @Composable
  override fun present(input: InputT): ModelT {
    val dispatcher = remember { BackGestureDispatcherPresenter.createNewInstance() }

    return returningCompositionLocalProvider(
      LocalBackGestureDispatcherPresenter provides dispatcher
    ) {
      LaunchedEffect(Unit) { backEvents.collect { dispatcher.onPredictiveBack(it) } }

      delegate.present(input)
    }
  }
}

/**
 * Wraps the receiver presenter with another presenter to provide a [BackGestureDispatcherPresenter]
 * as composition local. This is needed when your presenter uses [BackHandlerPresenter] or
 * [PredictiveBackHandlerPresenter] and requires the composition local.
 *
 * [backEvents] is an optional parameter to simulate back press events.
 */
@JvmName("withBackGestureDispatcherUnit")
public fun <InputT : Any, ModelT : BaseModel> MoleculePresenter<InputT, ModelT>
  .withBackGestureDispatcher(
  backEvents: SharedFlow<Unit> = MutableSharedFlow()
): MoleculePresenter<InputT, ModelT> =
  TestBackGestureDispatcherPresenter(this, backEvents.map { emptyFlow() })

/**
 * Wraps the receiver presenter with another presenter to provide a [BackGestureDispatcherPresenter]
 * as composition local. This is needed when your presenter uses [BackHandlerPresenter] or
 * [PredictiveBackHandlerPresenter] and requires the composition local.
 *
 * [backEvents] provides predictive back press events.
 */
public fun <InputT : Any, ModelT : BaseModel> MoleculePresenter<InputT, ModelT>
  .withBackGestureDispatcher(
  backEvents: SharedFlow<Flow<BackEventPresenter>>
): MoleculePresenter<InputT, ModelT> = TestBackGestureDispatcherPresenter(this, backEvents)
