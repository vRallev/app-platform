package software.ralf.app.platform.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Used within a [Presenter] to convert a [Flow] to a [StateFlow], e.g.
 *
 * ```
 * @Inject
 * class LoginPresenter(
 *     repository: Repository,
 *     presenterScope: PresenterCoroutineScope,
 * ) : Presenter<Model> {
 *
 *     override val model = repository.dataFlow
 *         .stateInPresenter(presenterScope) {
 *             Model(repository.dataFlow.value)
 *         }
 *
 *     data class Model(
 *         val data: Repository.Data
 *     ) : BaseModel
 * }
 * ```
 *
 * This is a convenience function and could be replaced by following line instead:
 * ```
 * .stateIn(presenterScope, SharingStarted.WhileSubscribed(), Model(repository.dataFlow.value))
 * ```
 *
 * [SharingStarted.WhileSubscribed] is chosen as default to cancel any upstream subscriptions as
 * soon as nobody collects the returned [StateFlow] anymore.
 */
public inline fun <T : BaseModel> Flow<T>.stateInPresenter(
  coroutineScope: CoroutineScope,
  crossinline default: () -> T,
): StateFlow<T> {
  return stateIn(coroutineScope, SharingStarted.WhileSubscribed(), default())
}
