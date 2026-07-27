package software.ralf.app.platform.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.listdetail.screen.ScreenSize
import software.ralf.app.platform.listdetail.screen.ScreenSizeProvider
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.template.ModelDelegate

/**
 * Adaptive feature presenter that chooses the phone or tablet presentation.
 *
 * Selection state is remembered here rather than app-scoped, so it lives exactly as long as this
 * presented feature and survives transitions between the two layouts.
 */
@Inject
@ContributesBinding(AppScope::class)
class ListDetailPresenterImpl(
  private val screenSizeProvider: ScreenSizeProvider,
  private val phonePresenter: PhoneListDetailPresenter,
  private val tabletPresenter: TabletListDetailPresenter,
) : ListDetailPresenter {
  @Composable
  override fun present(input: Unit): Model {
    val screenSize by screenSizeProvider.screenSize.collectAsState()
    val selectionState = remember { ListDetailSelectionState() }
    val model =
      if (screenSize.category == ScreenSize.Category.TABLET) {
        tabletPresenter.present(TabletListDetailPresenter.Input(selectionState))
      } else {
        phonePresenter.present(PhoneListDetailPresenter.Input(selectionState))
      }

    return Model(model)
  }

  /** Delegates renderer lookup to the model emitted by the active adaptive presentation. */
  data class Model(private val model: BaseModel) : BaseModel, ModelDelegate {
    override fun delegate(): BaseModel = model
  }
}
