package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.createGraph

class DesktopApp(val graph: AppGraph = createGraph<DesktopAppGraph>()) {
  @Composable
  fun Content(modifier: Modifier = Modifier) {
    AppContent(graph, modifier)
  }
}
