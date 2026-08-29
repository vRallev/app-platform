package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import software.ralf.circuit.listdetail.screen.ScreenSize
import software.ralf.circuit.listdetail.screen.ScreenSizeProvider

@Inject
class ListDetailPresenter(
  private val screenSizeProvider: ScreenSizeProvider,
  private val phonePresenterFactory: PhoneListDetailPresenter.Factory,
  private val tabletPresenterFactory: TabletListDetailPresenter.Factory,
) : Presenter<ListDetailPresenter.State> {
  @Composable
  override fun present(): State {
    val screenSize by screenSizeProvider.screenSize.collectAsState()
    val selectionState =
      rememberSaveable(saver = ListDetailSelectionState.Saver) {
        ListDetailSelectionState()
      }

    return if (screenSize.category == ScreenSize.Category.TABLET) {
      val presenter = remember(selectionState) { tabletPresenterFactory.create(selectionState) }
      State.Tablet(presenter.present())
    } else {
      val presenter = remember(selectionState) { phonePresenterFactory.create(selectionState) }
      State.Phone(presenter.present())
    }
  }

  sealed interface State : CircuitUiState {
    data class Phone(val state: PhoneListDetailPresenter.State) : State

    data class Tablet(val state: TabletListDetailPresenter.State) : State
  }
}
