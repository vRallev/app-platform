package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject

@Inject
class ListDetailUi(
  private val phoneUi: PhoneListDetailUi,
  private val tabletUi: TabletListDetailUi,
) : Ui<ListDetailPresenter.State> {
  @Composable
  override fun Content(state: ListDetailPresenter.State, modifier: Modifier) {
    when (state) {
      is ListDetailPresenter.State.Phone -> phoneUi.Content(state.state, modifier)
      is ListDetailPresenter.State.Tablet -> tabletUi.Content(state.state, modifier)
    }
  }
}
