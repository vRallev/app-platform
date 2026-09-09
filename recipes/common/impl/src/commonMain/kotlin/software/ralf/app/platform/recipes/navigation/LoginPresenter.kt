@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.requireNotNull
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/** Hosts the email and password presenters and owns navigation between them. */
class LoginPresenter : ComposePresenter<Unit, LoginPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = LocalBackstackScope.requireNotNull()
    var step by remember { mutableStateOf(Step.Email) }

    return when (step) {
      Step.Email -> {
        val emailPresenter = remember { EmailPresenter() }
        val emailModel = emailPresenter.present(Unit)

        Model.Email(email = emailModel.email, onNext = { step = Step.Password })
      }

      Step.Password -> {
        val passwordPresenter = remember { PasswordPresenter() }
        val passwordModel = passwordPresenter.present(Unit)

        Model.Password(
          password = passwordModel.password,
          onBack = { step = Step.Email },
          onDone = {
            // Leave this screen
            backstack.pop()
          },
        )
      }
    }
  }

  private enum class Step {
    Email,
    Password,
  }

  /** A credential-entry step and its navigation callbacks. */
  sealed interface Model : BaseModel {
    /** The email step. */
    data class Email(
      /** The presenter-owned email text. */
      val email: PresenterTextFieldState,
      /** Advances to the password step. */
      val onNext: () -> Unit,
    ) : Model

    /** The password step. */
    data class Password(
      /** The presenter-owned password text. */
      val password: PresenterTextFieldState,
      /** Returns to the email step. */
      val onBack: () -> Unit,
      /** Completes this presenter. */
      val onDone: () -> Unit,
    ) : Model
  }
}
