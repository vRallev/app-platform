package software.ralf.app.platform.sample.login

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/** A presenter to render the login screen. */
interface LoginPresenter : MoleculePresenter<Unit, LoginPresenter.Model> {
  /** The state of the login screen. */
  data class Model(
    /** Whether login is currently in progress. */
    val loginInProgress: Boolean,

    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit,
  ) : BaseModel

  /** All events that [LoginPresenter] can process. */
  sealed interface Event {
    /** Sent when the user presses the login button with the entered [userName]. */
    data class Login(val userName: String) : Event
  }
}
