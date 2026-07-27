package software.ralf.app.platform.listdetail

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
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.no_characters_available
import software.ralf.app.platform.listdetail.theme.AppTheme
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

/**
 * Two-pane renderer with a fixed-width list and flexible detail pane.
 *
 * Child models are rendered through [RendererFactory], keeping this layout independent of their
 * concrete Compose renderers.
 */
@Inject
@ContributesRenderer
class TabletListDetailRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<TabletListDetailPresenter.Model>() {
  @Composable
  override fun Compose(model: TabletListDetailPresenter.Model, modifier: Modifier) {
    Row(modifier.fillMaxSize().testTag("tabletListDetail")) {
      Surface(
        modifier = Modifier.width(384.dp).fillMaxSize(),
        color = AppTheme.colorScheme.surface,
      ) {
        rendererFactory
          .getComposeRenderer(model.listModel)
          .renderCompose(model.listModel, Modifier.fillMaxSize())
      }
      VerticalDivider()
      Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
        when (model) {
          is TabletListDetailPresenter.Model.Empty ->
            Text(
              text = stringResource(Res.string.no_characters_available),
              color = AppTheme.colorScheme.onSurfaceVariant,
              style = AppTheme.typography.bodyLarge,
            )
          is TabletListDetailPresenter.Model.Content ->
            rendererFactory
              .getComposeRenderer(model.detailModel)
              .renderCompose(model.detailModel, Modifier.fillMaxSize())
        }
      }
    }
  }
}
