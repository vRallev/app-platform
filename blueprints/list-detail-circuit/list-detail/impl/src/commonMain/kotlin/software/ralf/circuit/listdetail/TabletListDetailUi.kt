package software.ralf.circuit.listdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.no_characters_available
import software.ralf.circuit.listdetail.theme.AppTheme

@Inject
class TabletListDetailUi(
  private val listUi: CharacterListUi,
  private val detailUi: CharacterDetailUi,
) : Ui<TabletListDetailPresenter.State> {
  @Composable
  override fun Content(state: TabletListDetailPresenter.State, modifier: Modifier) {
    Row(modifier.fillMaxSize().testTag("tabletListDetail")) {
      Surface(
        modifier = Modifier.width(384.dp).fillMaxSize(),
        color = AppTheme.colorScheme.surface,
      ) {
        listUi.Content(state.listState, Modifier.fillMaxSize())
      }
      VerticalDivider()
      Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
          is TabletListDetailPresenter.State.Empty ->
            Text(
              text = stringResource(Res.string.no_characters_available),
              color = AppTheme.colorScheme.onSurfaceVariant,
              style = AppTheme.typography.bodyLarge,
            )
          is TabletListDetailPresenter.State.Content ->
            detailUi.Content(state.detailState, Modifier.fillMaxSize())
        }
      }
    }
  }
}
