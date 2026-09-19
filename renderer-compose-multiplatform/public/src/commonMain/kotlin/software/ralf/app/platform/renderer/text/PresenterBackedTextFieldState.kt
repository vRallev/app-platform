package software.ralf.app.platform.renderer.text

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

/**
 * Remembers a Compose Foundation [TextFieldState] backed by [presenterState].
 *
 * The returned [TextFieldState] is initialized from [presenterState]. Edits made by the user in the
 * renderer are copied back to [presenterState], and text changes made by the presenter are copied
 * into the returned [TextFieldState].
 *
 * Use this in Compose renderers when the presenter owns the text value but Compose Foundation owns
 * editing details such as cursor position and selection:
 * ```kotlin
 * data class Model(
 *   val query: PresenterTextFieldState,
 * ) : BaseModel
 *
 * @OptIn(ExperimentalAppPlatform::class)
 * @Composable
 * fun SearchContent(model: Model, modifier: Modifier = Modifier) {
 *   val queryState = rememberPresenterBackedTextFieldState(model.query)
 *
 *   BasicTextField(
 *     state = queryState,
 *     modifier = modifier,
 *   )
 * }
 * ```
 *
 * If the presenter replaces the text, the cursor is placed at the end of the new value. This avoids
 * preserving a renderer selection that may no longer be valid for the presenter's text.
 */
@ExperimentalAppPlatform
@Composable
public fun rememberPresenterBackedTextFieldState(
  presenterState: PresenterTextFieldState
): TextFieldState {
  val presenterText = presenterState.value
  val presenterTextState by rememberUpdatedState(presenterState)
  val textFieldState = rememberTextFieldState(presenterText)

  LaunchedEffect(textFieldState) {
    snapshotFlow { textFieldState.text.toString() }
      .collectLatest { text ->
        if (text != presenterTextState.value) {
          presenterTextState.replaceText(text)
        }
      }
  }

  LaunchedEffect(presenterText, textFieldState) {
    if (presenterText != textFieldState.text.toString()) {
      textFieldState.setTextAndPlaceCursorAtEnd(presenterText)
    }
  }

  return textFieldState
}
