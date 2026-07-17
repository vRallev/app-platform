package software.ralf.app.platform.template.navigation

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.template.templates.AppTemplate

@Inject
@ContributesBinding(AppScope::class)
class NavigationPresenterImpl(
  private val navigationHeaderPresenter: NavigationHeaderPresenter,
  private val navigationDetailPresenter: NavigationDetailPresenter,
) : NavigationPresenter {
  @Composable
  override fun present(input: Unit): BaseModel {
    val navigationBarModel = navigationHeaderPresenter.present(Unit)
    val navigationDetailModel = navigationDetailPresenter.present(Unit)
    return AppTemplate.HeaderDetailTemplate(navigationBarModel, navigationDetailModel)
  }
}
