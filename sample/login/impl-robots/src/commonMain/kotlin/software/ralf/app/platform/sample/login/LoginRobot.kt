package software.ralf.app.platform.sample.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.zacsweers.metro.AppScope
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.ComposeRobot

/** A test robot to verify interactions with the login screen written with Compose Multiplatform. */
@ContributesRobot(AppScope::class)
class LoginRobot : ComposeRobot() {

  private val loginButtonNode
    get() = compose.onNodeWithTag("loginButton")

  /** Verify that login button is displayed. */
  fun seeLoginButton() {
    loginButtonNode.assertIsDisplayed()
  }

  /** Clicks the login button and starts the login process. */
  fun clickLoginButton() {
    loginButtonNode.performClick()
  }
}
