package software.ralf.app.platform.presenter

import androidx.compose.runtime.Stable

/**
 * `Presenters` produce a stream of models that represents the state of this presenter. Concrete
 * model types are usually implemented as inner classes of the presenter, e.g.
 *
 * ```
 * class LoginPresenter : Presenter<Model> {
 *     data class Model(..) : BaseModel
 * }
 * ```
 *
 * Models must be immutable or satisfy the Compose stability contract by notifying composition when
 * public state changes. Mutating a previously returned model without observable state can produce
 * incorrect results or crashes. Immutable data classes are the common default.
 *
 * Using sealed hierarchies for models is common and allows to differentiate between states better:
 * ```
 * class LoginPresenter : Presenter<Model> {
 *     sealed interface Model : BaseModel {
 *         data object LoggedOut : Model
 *
 *         data class LoggedIn(
 *             val user: User,
 *         ) : Model
 *     }
 * }
 * ```
 *
 * State observers such as the UI layer communicate with the `Presenter` through events. Events are
 * returned through an `onEvent` callback in the model class and the `Presenter` handles the event:
 * ```
 * class LoginPresenter : Presenter<Model> {
 *     sealed interface Event {
 *         data object Logout : Event
 *
 *         data class ChangeName(
 *             val newName: String,
 *         ) : Event
 *     }
 *
 *     sealed interface Model : BaseModel {
 *         data object LoggedOut : Model
 *
 *         data class LoggedIn(
 *             val user: User,
 *             val onEvent: (Event) -> Unit
 *         ) : Model
 *     }
 * }
 * ```
 *
 * [BaseModel] is a marker interface for all models that can be used for extensions.
 */
@Stable public interface BaseModel
