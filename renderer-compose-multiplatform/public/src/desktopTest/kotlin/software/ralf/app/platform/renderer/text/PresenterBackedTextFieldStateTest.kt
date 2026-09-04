package software.ralf.app.platform.renderer.text

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.compose.text.PresenterTextFieldState

@OptIn(ExperimentalAppPlatform::class, ExperimentalTestApi::class)
class PresenterBackedTextFieldStateTest {
  @Test
  fun `initializes the renderer text state from the presenter text state`() {
    runComposeUiTest {
      val presenterState = PresenterTextFieldState("initial")
      var textFieldState: TextFieldState? = null

      setContent { textFieldState = rememberPresenterBackedTextFieldState(presenterState) }

      waitForIdle()
      assertThat(textFieldState!!.text.toString()).isEqualTo("initial")
    }
  }

  @Test
  fun `updates the presenter text state when the renderer text state changes`() {
    runComposeUiTest {
      val presenterState = PresenterTextFieldState("initial")
      var textFieldState: TextFieldState? = null

      setContent { textFieldState = rememberPresenterBackedTextFieldState(presenterState) }
      waitForIdle()

      runOnIdle { textFieldState!!.setTextAndPlaceCursorAtEnd("updated") }
      waitForIdle()

      assertThat(presenterState.value).isEqualTo("updated")
    }
  }

  @Test
  fun `updates the renderer text state when the presenter text state changes`() {
    runComposeUiTest {
      val presenterState = PresenterTextFieldState("initial")
      var textFieldState: TextFieldState? = null

      setContent { textFieldState = rememberPresenterBackedTextFieldState(presenterState) }
      waitForIdle()

      runOnIdle { presenterState.replaceText("updated") }
      waitForIdle()

      assertThat(textFieldState!!.text.toString()).isEqualTo("updated")
    }
  }
}
