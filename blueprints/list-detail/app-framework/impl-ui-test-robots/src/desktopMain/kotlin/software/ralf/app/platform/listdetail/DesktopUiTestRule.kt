@file:OptIn(ExperimentalTestApi::class)

package software.ralf.app.platform.listdetail

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpSize
import dev.zacsweers.metro.createGraphFactory
import kotlin.math.roundToInt
import software.ralf.app.platform.robot.internal.RobotInternals

/**
 * Desktop integration-test fixture that renders the production application at a controlled size.
 *
 * A fresh application, root scope, Metro graph, and Molecule presenter stream are created for each
 * test and destroyed after the Compose test scene closes.
 */
class DesktopUiTestRule {
  /** Runs [block] in the single-pane portrait window preset. */
  fun runPhoneRobotTest(block: ComposeUiTest.() -> Unit) {
    runRobotTest(windowSize = DesktopWindowSizes.phone, block = block)
  }

  /** Runs [block] in the two-pane landscape window preset. */
  fun runTabletRobotTest(block: ComposeUiTest.() -> Unit) {
    runRobotTest(windowSize = DesktopWindowSizes.tablet, block = block)
  }

  private fun runRobotTest(windowSize: DpSize, block: ComposeUiTest.() -> Unit) {
    val desktopApp = DesktopApp {
      createGraphFactory<TestDesktopAppGraph.Factory>().create(it)
    }
    RobotInternals.setRootScopeProvider(desktopApp)

    try {
      runDesktopComposeUiTest(
        width = windowSize.width.value.roundToInt(),
        height = windowSize.height.value.roundToInt(),
      ) {
        setContent { desktopApp.renderTemplates() }
        block()
      }
    } finally {
      RobotInternals.setRootScopeProvider(null)
      desktopApp.destroy()
    }
  }
}
