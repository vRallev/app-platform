@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.requireNotNull
import software.ralf.app.platform.presenter.compose.ComposePresenter

/** Hosts the email and password presenters and interprets their completion outputs. */
class LoginPresenter : ComposePresenter<Unit, BaseModel> {
  @Composable
  override fun present(input: Unit): BaseModel {
    val backstack = LocalBackstackScope.requireNotNull()
    var step by remember { mutableStateOf(Step.Email) }

    return when (step) {
      Step.Email -> {
        val emailPresenter = remember { EmailPresenter() }
        when (val model = emailPresenter.present(Unit)) {
          is EmailPresenter.Model.Content -> model
          is EmailPresenter.Model.Done -> {
            LaunchedEffect(Unit) { step = Step.Password }
            model
          }
        }
      }

      Step.Password -> {
        val passwordPresenter = remember { PasswordPresenter() }
        when (val model = passwordPresenter.present(Unit)) {
          is PasswordPresenter.Model.Content -> model
          is PasswordPresenter.Model.Back -> {
            LaunchedEffect(Unit) { step = Step.Email }
            model
          }

          is PasswordPresenter.Model.Done -> {
            LaunchedEffect(Unit) {
              // Leave this screen
              backstack.pop()
            }
            model
          }
        }
      }
    }
  }

  private enum class Step {
    Email,
    Password,
  }
}
