package software.ralf.circuit.listdetail

import androidx.compose.ui.window.ComposeUIViewController
import dev.zacsweers.metro.createGraph
import platform.UIKit.UIViewController

/** Creates a controller with its own graph; disposing its composition stops presenter work. */
@Suppress("unused")
fun mainViewController(): UIViewController {
  val graph = createGraph<IosAppGraph>()
  return ComposeUIViewController { AppContent(graph) }
}
