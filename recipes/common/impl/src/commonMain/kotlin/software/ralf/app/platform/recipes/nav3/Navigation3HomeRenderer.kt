@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.ralf.app.platform.recipes.nav3.Navigation3HomePresenter.Model
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

/** Renderer that integrates the presenter backstack with Navigation 3. */
@Inject
@ContributesRenderer
class Navigation3HomeRenderer(private val rendererFactory: RendererFactory) :
  PresenterBackstackRenderer<Model>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    rendererFactory.getComposeRenderer(model).renderCompose(model)
  }
}
