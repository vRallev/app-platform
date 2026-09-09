@file:OptIn(ExperimentalAppPlatform::class, ExperimentalMaterial3Api::class)

package software.ralf.app.platform.recipes.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app_platform.recipes.common.`impl`.generated.resources.Res
import app_platform.recipes.common.`impl`.generated.resources.any_input_accepted
import app_platform.recipes.common.`impl`.generated.resources.back
import app_platform.recipes.common.`impl`.generated.resources.done
import app_platform.recipes.common.`impl`.generated.resources.enter_password
import app_platform.recipes.common.`impl`.generated.resources.password
import org.jetbrains.compose.resources.stringResource
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.text.rememberPresenterBackedTextFieldState

/** Renders the password entry step. */
@ContributesRenderer
class PasswordRenderer : ComposeRenderer<PasswordPresenter.Model>() {
  @Composable
  override fun Compose(model: PasswordPresenter.Model, modifier: Modifier) {
    val content =
      when (model) {
        is PasswordPresenter.Model.Content -> model
        is PasswordPresenter.Model.Back -> model.content
        is PasswordPresenter.Model.Done -> model.content
      }

    Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
        Text(
          text = stringResource(Res.string.enter_password),
          style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedSecureTextField(
          state = rememberPresenterBackedTextFieldState(content.password),
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(Res.string.password)) },
          supportingText = { Text(stringResource(Res.string.any_input_accepted)) },
        )
        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.align(Alignment.End),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(onClick = content.onBack) {
            Text(stringResource(Res.string.back))
          }
          Button(onClick = content.onDone) { Text(stringResource(Res.string.done)) }
        }
      }
    }
  }
}
