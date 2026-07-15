package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.Presenter

/**
 * `MoleculePresenter` is a presenter that uses Compose core (don't confuse Compose core with
 * Compose UI, Compose UI is built on-top of Compose core) to create a [StateFlow] of models.
 * [Molecule](https://github.com/cashapp/molecule) is leveraged to turn the composable function
 * [present] into a `StateFlow<ModelT>`. By leveraging Compose we can turn reactive code built
 * on-top of Flow and its operators into imperative code using language statements like
 * `if-then-else`, `when` or `try-catch`.
 *
 * Note that [MoleculePresenter] itself doesn't extend the [Presenter] interface. Use
 * [launchMoleculePresenter] to transform a [MoleculePresenter] to a [Presenter]. To use another
 * [Presenter] within a [MoleculePresenter] you can inject the presenter directly and subscribe to
 * changes of [Presenter.model].
 *
 * `MoleculePresenters` typically are stateless, meaning they have no properties and are not marked
 * as singletons. If no input is used, then use [Unit] for [InputT]. A typical implementation may
 * look like:
 * ```
 * @Inject
 * class MyPresenter : MoleculePresenter<Unit, Model> {
 *
 *     @Composable
 *     override fun present(input: Unit): Model {
 *         ...
 *         return Model(...)
 *     }
 *
 *     data class Model(...) : BaseModel
 *
 * }
 * ```
 *
 * If a consumer like the UI layer should be able to send events back to the presenter, then the
 * [BaseModel] implementation typically has an `onEvent` callback lambda as last parameter:
 * ```
 * @Inject
 * class MyPresenter : MoleculePresenter<Unit, Model> {
 *
 *     @Composable
 *     override fun present(input: Unit): Model {
 *         var myData by remember { mutableStateOf("") }
 *
 *         ...
 *         return Model(...) { event ->
 *             when (event) {
 *                 is MyEvent -> {
 *                     // This will trigger recomposition and a new Model will be produced.
 *                     myData = event.moreData
 *                 }
 *             }
 *         }
 *     }
 *
 *     data class Model(
 *         ...,
 *         onEvent: (Event) -> Unit,
 *     ) : BaseModel
 *
 *     sealed interface Event {
 *         data class MyEvent(
 *             val moreData: String,
 *         ) : Event
 *     }
 * }
 * ```
 *
 * `MoleculePresenters` can host and embed other child presenters. To invoke them call [present]
 * inline. This is also the chance to pass inputs from one presenter to another. To avoid
 * instantiating presenters eagerly and only when they're actually needed, it's recommended to
 * inject them lazily:
 * ```
 * @Inject
 * class MyPresenter(
 *     private val userPresenter: () -> UserPresenter,
 *     private val loginPresenter: () -> LoginPresenter,
 * ) : MoleculePresenter<Unit, Model> {
 *
 *     @Composable
 *     override fun present(input: Unit): Model {
 *         if (condition) {
 *             val userPresenter = remember { userPresenter() }
 *             val userPresenterModel = userPresenter.present(Unit)
 *             // Use the returned model for further evaluation.
 *         } else {
 *             val loginPresenter = remember { loginPresenter() }
 *             val loginPresenterModel = loginPresenter.present("String input")
 *             // Use the returned model for further evaluation.
 *         }
 *
 *         return Model(...)
 *     }
 *
 *     data class Model(...) : BaseModel
 *
 * }
 * ```
 */
public interface MoleculePresenter<InputT : Any, ModelT : BaseModel> {
  /** Called every time state of the composable changes to produce a new model. */
  @Composable public fun present(input: InputT): ModelT
}
