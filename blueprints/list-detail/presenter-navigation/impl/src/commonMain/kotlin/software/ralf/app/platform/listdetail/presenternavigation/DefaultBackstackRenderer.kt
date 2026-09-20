@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail.presenternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.listdetail.templates.LocalAnimatedVisibilityScope
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.ralf.app.platform.renderer.Render

/**
 * Renders the active entry from [DefaultBackstackModel].
 *
 * Entry rendering remains polymorphic by resolving each [BaseModel] with [Render].
 */
@ContributesRenderer
class DefaultBackstackRenderer : PresenterBackstackRenderer<DefaultBackstackModel>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    // NavDisplay is implemented with AnimatedContent. Forward its entry-specific scope so child
    // renderers can coordinate shared elements with the outgoing or incoming navigation entry.
    CompositionLocalProvider(
      LocalAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current
    ) {
      Render(model)
    }
  }
}
