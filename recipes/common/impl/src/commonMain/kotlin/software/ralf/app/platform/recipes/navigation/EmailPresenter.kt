@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/** Owns the state for the email step. */
class EmailPresenter : ComposePresenter<Unit, EmailPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val email = remember { PresenterTextFieldState() }

    return Model(email = email)
  }

  /** The presenter-owned email text. */
  data class Model(
    /** The current email text. */
    val email: PresenterTextFieldState
  ) : BaseModel
}
