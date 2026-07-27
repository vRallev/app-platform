package software.ralf.app.platform.listdetail

import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * Android UI-test rule that launches the real activity with the integration-test Metro graph.
 *
 * The device determines the adaptive presentation. System animations are disabled for deterministic
 * assertions, while Compose animations remain available to the app.
 */
class AndroidUiTestRule : TestRule {
  private val activityRule = ActivityScenarioRule(MainActivity::class.java)
  private val composeTestRule =
    AndroidComposeTestRule(
      activityRule = activityRule,
      activityProvider = { rule -> getActivity(rule) },
    )

  override fun apply(base: Statement, description: Description): Statement {
    val wrappedBase =
      object : Statement() {
        override fun evaluate() {
          setSystemAnimations(enabled = false)
          try {
            base.evaluate()
          } finally {
            destroyRootScope()
            setSystemAnimations(enabled = true)
          }
        }
      }

    return composeTestRule.apply(wrappedBase, description)
  }

  /** Runs [block] after the activity reaches an idle Compose state. */
  fun runRobotTest(block: ComposeTestRule.() -> Unit) {
    composeTestRule.waitForIdle()
    composeTestRule.block()
  }

  private fun getActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
    var activity: MainActivity? = null
    rule.scenario.onActivity { currentActivity -> activity = currentActivity }
    return checkNotNull(activity) { "Activity was not set in the ActivityScenarioRule." }
  }

  private fun destroyRootScope() {
    val rootScopeProvider =
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        as RootScopeProvider
    rootScopeProvider.rootScope.destroy()
  }

  private fun setSystemAnimations(enabled: Boolean) {
    val value = if (enabled) "1.0" else "0.0"
    InstrumentationRegistry.getInstrumentation().uiAutomation.run {
      executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE $value")
    }
  }
}
