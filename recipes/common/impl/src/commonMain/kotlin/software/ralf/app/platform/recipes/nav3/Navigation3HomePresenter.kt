@file:OptIn(ExperimentalAppPlatform::class)
@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackModel
import software.ralf.app.platform.presenter.backstack.nav3.presenterBackstack
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.recipes.appbar.AppBarConfig
import software.ralf.app.platform.recipes.appbar.AppBarConfigModel
import software.ralf.app.platform.recipes.nav3.Navigation3HomePresenter.Model

/** Presenter that hosts a Navigation 3 presenter backstack. */
@Inject
class Navigation3HomePresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    return presenterBackstack(Navigation3ChildPresenter(index = 0)) { backstack ->
      Model(backstack = backstack, onBack = { pop() })
    }
  }

  data class Model(override val backstack: List<BaseModel>, override val onBack: () -> Unit) :
    PresenterBackstackModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig = AppBarConfig(title = "Navigation3")
  }
}
