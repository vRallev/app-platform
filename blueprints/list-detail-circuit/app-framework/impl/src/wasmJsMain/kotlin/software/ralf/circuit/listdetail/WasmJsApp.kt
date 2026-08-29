package software.ralf.circuit.listdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.createGraph

class WasmJsApp(val graph: AppGraph = createGraph<WasmJsAppGraph>()) {
  @Composable
  fun Content(modifier: Modifier = Modifier) {
    AppContent(graph, modifier)
  }
}
