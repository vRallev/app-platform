package software.ralf.app.platform.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import software.ralf.app.platform.presenter.BaseModel

/**
 * Renders [model] using [LocalRendererFactory]. Use a distinct [rendererId] to cache separate
 * renderers for the same model type.
 *
 * Call this inside a renderer obtained from a Compose renderer factory, or provide
 * [LocalRendererFactory] explicitly in tests and previews.
 */
@Composable
public fun Render(model: BaseModel, modifier: Modifier = Modifier, rendererId: Int = 0) {
  val factory =
    checkNotNull(LocalRendererFactory.current) {
      "No RendererFactory provided. Render through ComposeRendererFactory or " +
        "ComposeAndroidRendererFactory, or provide LocalRendererFactory explicitly."
    }
  factory.renderCompose(model, modifier, rendererId)
}
