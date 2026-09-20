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
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.Render

/**
 * A Compose renderer implementation for templates used in the sample application.
 *
 * [Render] resolves child renderers from the factory provided by the platform entry point.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@ContributesRenderer
class ComposeAppTemplateRenderer : ComposeRenderer<AppTemplate>() {
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
    Render(template.model)
  }

  @Composable
  private fun HeaderDetail(template: AppTemplate.HeaderDetailTemplate) {
    Column {
      Row(Modifier.Companion.weight(1f)) {
        Render(template.header)
      }
      Row(Modifier.Companion.weight(5f)) {
        Render(template.detail)
      }
    }
  }
}
