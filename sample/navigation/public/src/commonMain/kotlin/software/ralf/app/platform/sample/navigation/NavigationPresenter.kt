package software.ralf.app.platform.sample.navigation

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter

/**
 * A presenter that hosts other presenters and returns their models. For that reason this presenter
 * doesn't have its own [BaseModel] type and returns [BaseModel].
 */
interface NavigationPresenter : ComposePresenter<Unit, BaseModel>
