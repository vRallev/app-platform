@file:OptIn(ExperimentalAppPlatform::class, ExperimentalMaterial3Api::class)

package software.ralf.app.platform.recipes.retained

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app_platform.recipes.common.`impl`.generated.resources.Res
import app_platform.recipes.common.`impl`.generated.resources.any_input_accepted
import app_platform.recipes.common.`impl`.generated.resources.email
import app_platform.recipes.common.`impl`.generated.resources.enter_email
import app_platform.recipes.common.`impl`.generated.resources.next
import org.jetbrains.compose.resources.stringResource
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.text.rememberPresenterBackedTextFieldState

/** Renders the email entry step. */
@ContributesRenderer
class EmailRenderer : ComposeRenderer<EmailPresenter.Model>() {
  @Composable
  override fun Compose(model: EmailPresenter.Model, modifier: Modifier) {
    val content =
      when (model) {
        is EmailPresenter.Model.Content -> model
        is EmailPresenter.Model.Done -> model.content
      }

    Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
        Text(
          text = stringResource(Res.string.enter_email),
          style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
          state = rememberPresenterBackedTextFieldState(content.email),
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(Res.string.email)) },
          supportingText = { Text(stringResource(Res.string.any_input_accepted)) },
          lineLimits = TextFieldLineLimits.SingleLine,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = content.onNext, modifier = Modifier.align(Alignment.End)) {
          Text(stringResource(Res.string.next))
        }
      }
    }
  }
}
