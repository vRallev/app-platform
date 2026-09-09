package software.ralf.app.platform.recipes.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app_platform.recipes.common.`impl`.generated.resources.Res
import app_platform.recipes.common.`impl`.generated.resources.retained_presenter_state
import org.jetbrains.compose.resources.stringResource
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.recipes.landing.LandingPresenter.Model
import software.ralf.app.platform.renderer.ComposeRenderer

/** Renders the content for [LandingPresenter] on screen. */
@ContributesRenderer
class LandingRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(onClick = { model.onEvent(LandingPresenter.Event.AddPresenterToBackstack) }) {
        Text("Add presenter to backstack")
      }
      Button(
        onClick = { model.onEvent(LandingPresenter.Event.MenuPresenter) },
        modifier = Modifier.padding(top = 12.dp),
      ) {
        Text("Menu presenter")
      }
      Button(
        onClick = { model.onEvent(LandingPresenter.Event.Navigation3) },
        modifier = Modifier.padding(top = 12.dp),
      ) {
        Text("Navigation3")
      }
      Button(
        onClick = { model.onEvent(LandingPresenter.Event.RetainedPresenterState) },
        modifier = Modifier.padding(top = 12.dp),
      ) {
        Text(stringResource(Res.string.retained_presenter_state))
      }
    }
  }
}
