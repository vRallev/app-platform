package software.ralf.circuit.listdetail

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Launches the production activity without changing the device's animation settings. */
class AndroidUiTestRule : TestRule {
  private val activityRule = ActivityScenarioRule(MainActivity::class.java)
  private val composeTestRule =
    AndroidComposeTestRule(
      activityRule = activityRule,
      activityProvider = { rule -> getActivity(rule) },
    )

  override fun apply(base: Statement, description: Description): Statement {
    return composeTestRule.apply(base, description)
  }

  fun runTest(block: ComposeTestRule.() -> Unit) {
    composeTestRule.waitForIdle()
    composeTestRule.block()
  }

  fun recreateActivity() {
    composeTestRule.waitForIdle()
    activityRule.scenario.recreate()
    composeTestRule.waitForIdle()
  }

  fun pressSystemBack() {
    composeTestRule.waitForIdle()
    activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    composeTestRule.waitForIdle()
  }

  private fun getActivity(rule: ActivityScenarioRule<MainActivity>): MainActivity {
    var activity: MainActivity? = null
    rule.scenario.onActivity { activity = it }
    return checkNotNull(activity) { "Activity was not set in the ActivityScenarioRule." }
  }
}
