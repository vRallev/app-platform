package software.ralf.app.platform.presenter.compose.text

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import software.ralf.app.platform.ExperimentalAppPlatform

/**
 * Presenter-owned text input state.
 *
 * Use this when a presenter needs to own the current text for an input field while the renderer
 * still needs a platform UI text state, such as Compose Foundation's `TextFieldState`, for cursor,
 * selection, and editing behavior. [PresenterTextFieldState] keeps only the text value, making it
 * suitable for presenter models without depending on Compose Foundation.
 *
 * In a Compose presenter, remember one instance for each logical text field and expose it through
 * the model:
 * ```kotlin
 * @OptIn(ExperimentalAppPlatform::class)
 * class SearchPresenter : ComposePresenter<Unit, SearchPresenter.Model> {
 *   @Composable
 *   override fun present(input: Unit): Model {
 *     val query = remember { PresenterTextFieldState() }
 *
 *     return Model(
 *       query = query,
 *       clearQuery = query::clearText,
 *     )
 *   }
 *
 *   data class Model(
 *     val query: PresenterTextFieldState,
 *     val clearQuery: () -> Unit,
 *   ) : BaseModel
 * }
 * ```
 *
 * Compose renderers can connect this presenter state to a Compose Foundation text field with
 * `rememberPresenterBackedTextFieldState()`.
 */
@ExperimentalAppPlatform
@Stable
public class PresenterTextFieldState(initialText: String = "") : State<String> {
  private var text by mutableStateOf(initialText)

  override val value: String
    get() = text

  /** Replaces the current text value. */
  public fun replaceText(text: String) {
    this.text = text
  }

  /** Clears the current text value. */
  public fun clearText() {
    replaceText("")
  }
}
