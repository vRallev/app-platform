package software.ralf.app.platform.presenter

import kotlinx.coroutines.flow.StateFlow

/**
 * A presenter is the glue between our business logic and UI. A presenter injects service objects,
 * data repositories and other presenters to compute a model to represent what should be shown to
 * the user. Presenters are reactive. If its internal state or state of injected dependencies
 * change, then a new model is emitted.
 *
 * Presenters are composable, meaning that one presenter can inject other presenters and combine
 * their emitted models to a single model. This enables to implement model-driven navigation.
 *
 * By decoupling presenters from UI and Android components like Activities, Fragments and ViewModels
 * we make them easier to test. Business logic and UI integration can evolve independently.
 *
 * Events from the UI layer flow back to the presenter inform of callbacks provided by the model:
 * ```
 * class LoginPresenter : Presenter<Model> {
 *     data class Model(
 *         val name: String,
 *         val onEvent: (Event) -> Unit
 *     ) : BaseModel
 *
 *     sealed interface Event {
 *         object OnNameClick : Event
 *     }
 * }
 * ```
 *
 * Presenters can be implemented with any framework or by hand. Most commonly we use
 * `MoleculePresenter`, which can be transformed into a [Presenter] with `launchMoleculePresenter`.
 * A direct implementation of this interface could look like this:
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
 */
public interface Presenter<ModelT : BaseModel> {
  /** The StateFlow of [ModelT] that the Presenter outputs. */
  public val model: StateFlow<ModelT>
}
