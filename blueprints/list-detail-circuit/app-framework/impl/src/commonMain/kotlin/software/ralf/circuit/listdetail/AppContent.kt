package software.ralf.circuit.listdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import software.ralf.circuit.listdetail.templates.AppScaffold

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent(graph: AppGraph, modifier: Modifier = Modifier) {
  CircuitCompositionLocals(graph.circuit) {
    AppScaffold(graph.screenSizeProvider, modifier) {
      SharedElementTransitionLayout {
        CircuitContent(ListDetailScreen)
      }
    }
  }
}
