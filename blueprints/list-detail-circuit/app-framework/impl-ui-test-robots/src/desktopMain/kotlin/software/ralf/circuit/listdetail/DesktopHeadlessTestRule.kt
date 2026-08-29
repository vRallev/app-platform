package software.ralf.circuit.listdetail

import androidx.compose.ui.unit.DpSize
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.test
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.test.runTest
import software.ralf.circuit.listdetail.screen.ScreenSize

/** Exercises the production graph and Circuit presenter tree without a Compose UI scene. */
class DesktopHeadlessTestRule {
  fun runPhoneTest(block: suspend CircuitReceiveTurbine<ListDetailPresenter.State>.() -> Unit) {
    runHeadlessTest(windowSize = DesktopWindowSizes.phone, block = block)
  }

  fun runTabletTest(block: suspend CircuitReceiveTurbine<ListDetailPresenter.State>.() -> Unit) {
    runHeadlessTest(windowSize = DesktopWindowSizes.tablet, block = block)
  }

  private fun runHeadlessTest(
    windowSize: DpSize,
    block: suspend CircuitReceiveTurbine<ListDetailPresenter.State>.() -> Unit,
  ) {
    runTest {
      val graph = createGraph<DesktopAppGraph>()
      graph.screenSizeProvider.update(ScreenSize.from(windowSize.width, windowSize.height))

      @Suppress("UNCHECKED_CAST")
      val presenter =
        checkNotNull(graph.circuit.presenter(ListDetailScreen, Navigator.NoOp)) {
          "The production Circuit graph must provide the list-detail presenter."
        }
          as Presenter<ListDetailPresenter.State>

      presenter.test(block = block)
    }
  }
}
