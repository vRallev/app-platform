package software.ralf.app.platform.sample.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.sample.login.LoginPresenter.Model

/** Renders the content for [LoginPresenter] on screen using Compose Multiplatform. */
@ContributesRenderer
class LoginRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (model.loginInProgress) {
        CircularProgressIndicator(modifier = Modifier.width(64.dp).testTag("loginProgress"))
      } else {
        Button(
          onClick = { model.onEvent(LoginPresenter.Event.Login("Imagine a text field")) },
          modifier = Modifier.testTag("loginButton"),
        ) {
          Text("Login")
        }
      }
    }
  }
}
