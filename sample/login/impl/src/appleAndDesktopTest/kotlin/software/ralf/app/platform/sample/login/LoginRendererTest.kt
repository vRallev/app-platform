package software.ralf.app.platform.sample.login

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Executes the [LoginRenderer] in unit tests for iOS and Desktop. Note that this test cannot run on
 * Android, because there the test would need to run on an emulator.
 *
 * This test highlights that `ComposeRenderers` are testable in isolation.
 */
@ExperimentalTestApi
class LoginRendererTest {

  @Test
  fun `the login button is rendered when not logging in`() {
    runComposeUiTest {
      setContent {
        val renderer = LoginRenderer()
        renderer.renderCompose(LoginPresenter.Model(loginInProgress = false) {})
      }

      onNodeWithTag("loginProgress").assertDoesNotExist()
      onNodeWithTag("loginButton").assertIsDisplayed()
    }
  }

  @Test
  fun `the progress is shown while logging in`() {
    runComposeUiTest {
      setContent {
        val renderer = LoginRenderer()
        renderer.renderCompose(LoginPresenter.Model(loginInProgress = true) {})
      }

      onNodeWithTag("loginProgress").assertIsDisplayed()
      onNodeWithTag("loginButton").assertDoesNotExist()
    }
  }
}
