package software.ralf.app.platform.renderer

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule

class TestActivity : ComponentActivity()

// Borrowed from AndroidComposeTestRule.
fun <A : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<A>): A {
  var activity: A? = null
  rule.scenario.onActivity { activity = it }

  return with(activity) {
    checkNotNull(this) { "Activity was not set in the ActivityScenarioRule!" }
  }
}
