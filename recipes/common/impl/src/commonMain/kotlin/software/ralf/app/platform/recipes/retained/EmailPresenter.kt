@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/** Owns the retained state for the email step. */
class EmailPresenter : ComposePresenter<Unit, EmailPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    var done by remember { mutableStateOf(false) }
    val email = retain { PresenterTextFieldState() }
    val content = Model.Content(email = email, onNext = { done = true })

    return if (done) Model.Done(content) else content
  }

  /** The email entry state or its completion output. */
  sealed interface Model : BaseModel {
    /** The active email entry state. */
    data class Content(
      /** The current email text. */
      val email: PresenterTextFieldState,
      /** Completes the email step. */
      val onNext: () -> Unit,
    ) : Model

    /** Indicates that email entry is complete. */
    data class Done(
      /** The last email entry state. */
      val content: Content
    ) : Model
  }
}
