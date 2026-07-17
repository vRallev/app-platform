package software.ralf.app.platform.template.templates

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

/**
 * A Compose renderer implementation for templates used in the sample application.
 *
 * [rendererFactory] is used to get the [software.ralf.app.platform.renderer.Renderer] for the
 * [software.ralf.app.platform.presenter.BaseModel] wrapped in the template.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Inject
@ContributesRenderer
class ComposeAppTemplateRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<AppTemplate>() {
  @Composable
  override fun Compose(model: AppTemplate, modifier: Modifier) {
    MaterialTheme {
      Box(modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        when (model) {
          is AppTemplate.FullScreenTemplate -> FullScreen(model)
          is AppTemplate.HeaderDetailTemplate -> HeaderDetail(model)
        }
      }
    }
  }

  @Composable
  private fun FullScreen(template: AppTemplate.FullScreenTemplate) {
    val renderer = rendererFactory.getComposeRenderer(template.model)
    renderer.renderCompose(template.model)
  }

  @Composable
  private fun HeaderDetail(template: AppTemplate.HeaderDetailTemplate) {
    Column {
      Row(Modifier.Companion.weight(1f)) {
        rendererFactory.getComposeRenderer(template.header).renderCompose(template.header)
      }
      Row(Modifier.Companion.weight(5f)) {
        rendererFactory.getComposeRenderer(template.detail).renderCompose(template.detail)
      }
    }
  }
}
