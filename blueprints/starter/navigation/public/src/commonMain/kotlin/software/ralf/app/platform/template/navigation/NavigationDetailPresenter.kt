package software.ralf.app.platform.template.navigation

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/** Presenter responsible for the state of the main content area beneath the navigation header. */
interface NavigationDetailPresenter : MoleculePresenter<Unit, NavigationDetailPresenter.Model> {
  data class Model(val exampleValue: Int, val exampleCount: Int) : BaseModel
}
