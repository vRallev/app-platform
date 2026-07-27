@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail.presenternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.listdetail.templates.LocalAnimatedVisibilityScope
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

/**
 * Renders the active entry from [DefaultBackstackModel].
 *
 * Entry rendering remains polymorphic by resolving each [BaseModel] through [RendererFactory].
 */
@Inject
@ContributesRenderer
class DefaultBackstackRenderer(private val rendererFactory: RendererFactory) :
  PresenterBackstackRenderer<DefaultBackstackModel>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    // NavDisplay is implemented with AnimatedContent. Forward its entry-specific scope so child
    // renderers can coordinate shared elements with the outgoing or incoming navigation entry.
    CompositionLocalProvider(
      LocalAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current
    ) {
      rendererFactory.getComposeRenderer(model).renderCompose(model)
    }
  }
}
