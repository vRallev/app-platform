@file:OptIn(ExperimentalTestApi::class)

package software.ralf.circuit.listdetail

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpSize
import dev.zacsweers.metro.createGraph
import kotlin.math.roundToInt

/** Renders the production Circuit application in a fresh Compose scene and Metro graph. */
class DesktopUiTestRule {
  fun runPhoneTest(block: ComposeUiTest.() -> Unit) {
    runTest(windowSize = DesktopWindowSizes.phone, block = block)
  }

  fun runTabletTest(block: ComposeUiTest.() -> Unit) {
    runTest(windowSize = DesktopWindowSizes.tablet, block = block)
  }

  private fun runTest(windowSize: DpSize, block: ComposeUiTest.() -> Unit) {
    val graph = createGraph<DesktopAppGraph>()

    runDesktopComposeUiTest(
      width = windowSize.width.value.roundToInt(),
      height = windowSize.height.value.roundToInt(),
    ) {
      setContent { AppContent(graph) }
      block()
    }
  }
}
