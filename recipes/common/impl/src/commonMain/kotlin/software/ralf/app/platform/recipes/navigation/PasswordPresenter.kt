@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/** Owns the state for the password step. */
class PasswordPresenter : ComposePresenter<Unit, PasswordPresenter.Model> {
  @Composable
  override fun present(input: Unit): Model {
    var completion by remember { mutableStateOf<Completion?>(null) }
    val password = remember { PresenterTextFieldState() }
    val content =
      Model.Content(
        password = password,
        onBack = { completion = Completion.Back },
        onDone = { completion = Completion.Done },
      )

    return when (completion) {
      null -> content
      Completion.Back -> Model.Back(content)
      Completion.Done -> Model.Done(content)
    }
  }

  private enum class Completion {
    Back,
    Done,
  }

  /** The password entry state or one of its completion outputs. */
  sealed interface Model : BaseModel {
    /** The active password entry state. */
    data class Content(
      /** The current password text. */
      val password: PresenterTextFieldState,
      /** Completes the password step by returning to email entry. */
      val onBack: () -> Unit,
      /** Completes the presenter-navigation recipe. */
      val onDone: () -> Unit,
    ) : Model

    /** Indicates that password entry completed by returning to email entry. */
    data class Back(
      /** The last password entry state. */
      val content: Content
    ) : Model

    /** Indicates that password entry and the presenter-navigation recipe are complete. */
    data class Done(
      /** The last password entry state. */
      val content: Content
    ) : Model
  }
}
