@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/** Owns the retained state for the password step. */
class PasswordPresenter : ComposePresenter<Unit, PasswordPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    val password = retain { PresenterTextFieldState() }

    return Model(password = password)
  }

  /** The presenter-owned password text. */
  data class Model(
    /** The current password text. */
    val password: PresenterTextFieldState
  ) : BaseModel
}
