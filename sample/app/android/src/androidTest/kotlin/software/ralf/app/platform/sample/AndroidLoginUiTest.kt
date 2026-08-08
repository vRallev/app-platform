package software.ralf.app.platform.sample

import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.robot.ComposeInteractionsProvider
import software.ralf.app.platform.robot.composeRobot
import software.ralf.app.platform.robot.waitUntilCatching
import software.ralf.app.platform.sample.login.LoginRobot
import software.ralf.app.platform.sample.user.UserPageRobot
import software.ralf.app.platform.scope.RootScopeProvider

/** This class implements [ComposeInteractionsProvider] to make it easier to call [composeRobot]. */
class AndroidLoginUiTest : ComposeInteractionsProvider {

  @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @get:Rule
  val composeTestRule =
    AndroidComposeTestRule(activityRule, activityProvider = { getActivityFromTestRule(it) })

  override val semanticsNodeInteractionsProvider: SemanticsNodeInteractionsProvider
    get() = composeTestRule

  @Before
  fun before() {
    setAnimations(enabled = false)
  }

  @After
  fun after() {
    val rootScopeProvider =
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        as RootScopeProvider

    // Good hygiene to clean everything up.
    rootScopeProvider.rootScope.destroy()
    setAnimations(enabled = true)
  }

  @Test
  fun a_user_logs_in_and_opens_the_profile_picture() {
    composeRobot<LoginRobot> {
      seeLoginButton()
      clickLoginButton()
    }

    waitUntilCatching("login finished", timeout = 4.seconds) {
      composeRobot<UserPageRobot> {
        seeUserId()
        seeProfilePicture(fullScreen = false)
      }
    }

    // Note that this code doesn't run within the `waitUntilCatching` on purpose. The code
    // above waits until we're logged in and retries the operation until the UI displayed. The
    // operations below should not be retried.

    composeRobot<UserPageRobot> { clickProfilePicture() }

    waitUntilCatching("profile picture opened fullscreen", timeout = 2.seconds) {
      composeRobot<UserPageRobot> { seeProfilePicture(fullScreen = true) }
    }

    composeRobot<UserPageRobot> { clickProfilePicture() }

    waitUntilCatching("profile picture closed fullscreen", timeout = 2.seconds) {
      composeRobot<UserPageRobot> { seeProfilePicture(fullScreen = false) }
    }
  }

  @Test
  fun a_back_press_logs_out_the_user_early() {
    composeRobot<LoginRobot> {
      seeLoginButton()
      clickLoginButton()
    }

    waitUntilCatching("login finished", timeout = 4.seconds) {
      composeRobot<UserPageRobot> {
        seeUserId()
        seeProfilePicture(fullScreen = false)
      }
    }

    Espresso.pressBack()

    waitUntilCatching("logout finished", timeout = 1.seconds) {
      composeRobot<LoginRobot> { seeLoginButton() }
    }
  }

  // Borrowed from AndroidComposeTestRule.
  private fun <A : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<A>): A {
    var activity: A? = null
    rule.scenario.onActivity { activity = it }

    return with(activity) {
      checkNotNull(this) { "Activity was not set in the ActivityScenarioRule!" }
    }
  }

  private fun setAnimations(enabled: Boolean) {
    val value = if (enabled) "1.0" else "0.0"
    InstrumentationRegistry.getInstrumentation().uiAutomation.run {
      executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE $value")
    }
  }
}
