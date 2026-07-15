@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.nav3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.recipes.nav3.Navigation3ChildPresenter.Model
import software.ralf.app.platform.renderer.ComposeRenderer

@ContributesRenderer
class Navigation3ChildRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Box(modifier = Modifier.fillMaxSize().background(Color(model.color))) {
      Column {
        Text("Index: ${model.index}")
        Text("Count: ${model.counter}")
      }

      Button(
        onClick = { model.onEvent(Navigation3ChildPresenter.Event.AddPresenter) },
        modifier = Modifier.align(Alignment.Center),
      ) {
        Text("Add Presenter")
      }
    }
  }
}
