@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retainManagedRetainedValuesStore
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.requireNotNull
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState
import software.ralf.app.platform.presenter.compose.withLocalRetainedValuesStore

/** Hosts the email and password presenters and interprets their navigation outputs. */
class RetainedStatePresenter : ComposePresenter<Unit, RetainedStatePresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = LocalBackstackScope.requireNotNull()
    var step by remember { mutableStateOf(Step.Email) }
    val emailRetainedValuesStore = key(Step.Email) { retainManagedRetainedValuesStore() }
    val passwordRetainedValuesStore = key(Step.Password) { retainManagedRetainedValuesStore() }

    return when (step) {
      Step.Email ->
        withLocalRetainedValuesStore(emailRetainedValuesStore) {
          val emailPresenter = remember { EmailPresenter() }
          val emailModel = emailPresenter.present(Unit)

          Model.Email(email = emailModel.email, onNext = { step = Step.Password })
        }

      Step.Password ->
        withLocalRetainedValuesStore(passwordRetainedValuesStore) {
          val passwordPresenter = remember { PasswordPresenter() }
          val passwordModel = passwordPresenter.present(Unit)

          Model.Password(
            password = passwordModel.password,
            onBack = { step = Step.Email },
            onDone = { backstack.pop() },
          )
        }
    }
  }

  private enum class Step {
    Email,
    Password,
  }

  /** A retained credential-entry step and its navigation outputs. */
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
