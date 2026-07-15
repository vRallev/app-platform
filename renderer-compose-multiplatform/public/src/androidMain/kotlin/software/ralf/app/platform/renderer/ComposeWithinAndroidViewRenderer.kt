package software.ralf.app.platform.renderer

import androidx.compose.runtime.Composable
import software.ralf.app.platform.presenter.BaseModel

/**
 * A renderer that allows you to embed a [BaseComposeRenderer] within a [ViewRenderer]. All [render]
 * calls are forwarded to the [composeRenderer]. The Compose UI hierarchy is embedded within the
 * Android View hierarchy.
 */
internal class ComposeWithinAndroidViewRenderer<in ModelT : BaseModel>(
  private val composeRenderer: BaseComposeRenderer<ModelT>
) : BaseComposeWithinAndroidViewRenderer<ModelT>() {
  @Composable
  override fun Compose(model: ModelT) {
    composeRenderer.renderCompose(model)
  }
}
