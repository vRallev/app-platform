package software.ralf.app.platform.template.navigation

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Presenter responsible for the state of the top navigation bar (header).
 *
 * This typically controls high-level UI elements such as titles, toggle buttons, or contextual
 * actions that affect the overall screen.
 */
interface NavigationHeaderPresenter : MoleculePresenter<Unit, NavigationHeaderPresenter.Model> {
  data class Model(val clickedCount: Int, val onEvent: (Event) -> Unit) : BaseModel

  /** Events that can be triggered by the UI layer (Renderer) and processed by the Presenter. */
  sealed interface Event {
    data object Clicked : Event
  }
}
