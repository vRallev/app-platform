@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.ralf.app.platform.recipes.nav3.Navigation3HomePresenter.Model
import software.ralf.app.platform.renderer.Render

/** Renderer that integrates the presenter backstack with Navigation 3. */
@ContributesRenderer
class Navigation3HomeRenderer : PresenterBackstackRenderer<Model>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    Render(model)
  }
}
