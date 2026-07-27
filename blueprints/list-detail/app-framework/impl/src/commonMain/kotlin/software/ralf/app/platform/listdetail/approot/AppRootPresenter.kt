package software.ralf.app.platform.listdetail.approot

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.listdetail.ListDetailPresenter
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/** Root presenter that exposes the list-detail feature to the application template. */
@Inject
class AppRootPresenter(private val listDetailPresenter: ListDetailPresenter) :
  MoleculePresenter<Unit, BaseModel> {
  @Composable
  override fun present(input: Unit): BaseModel {
    return listDetailPresenter.present(Unit)
  }
}
