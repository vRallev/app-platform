package software.ralf.app.platform.listdetail

import androidx.compose.ui.unit.DpSize
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.listdetail.screen.DefaultScreenSizeProvider
import software.ralf.app.platform.listdetail.screen.ScreenSize
import software.ralf.app.platform.listdetail.templates.AppTemplate
import software.ralf.app.platform.presenter.molecule.moleculeScope
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/**
 * Desktop integration-test fixture that observes production templates without rendering UI.
 *
 * Each test starts a fresh application, production-backed Metro graph, and root presenter stream.
 * The production window preset is reported directly to the screen-size provider before the stream
 * starts. A test-owned Molecule scope produces template emissions under the test scheduler,
 * allowing tests to exercise either adaptive presentation without a Compose scene or polling.
 */
class DesktopHeadlessTestRule {
  /** Runs [block] against the templates produced for the production phone window preset. */
  fun runPhoneTest(block: suspend ReceiveTurbine<AppTemplate>.() -> Unit) {
    runHeadlessTest(windowSize = DesktopWindowSizes.phone, block = block)
  }

  /** Runs [block] against the templates produced for the production tablet window preset. */
  fun runTabletTest(block: suspend ReceiveTurbine<AppTemplate>.() -> Unit) {
    runHeadlessTest(windowSize = DesktopWindowSizes.tablet, block = block)
  }

  private fun runHeadlessTest(
    windowSize: DpSize,
    block: suspend ReceiveTurbine<AppTemplate>.() -> Unit,
  ) {
    runTest {
      val application = Application()
      application.create(createGraphFactory<TestDesktopAppGraph.Factory>().create(application))

      val graph = application.rootScope.metroDependencyGraph<Graph>()
      graph.screenSizeProvider.update(ScreenSize.from(windowSize.width, windowSize.height))
      val templateProvider = graph.templateProviderInternalFactory.create(moleculeScope())

      try {
        templateProvider.templates.test { block() }
      } finally {
        templateProvider.cancel()
        application.destroy()
      }
    }
  }

  /** Production dependencies needed to configure and observe the headless application. */
  @ContributesTo(AppScope::class)
  interface Graph {
    /** App-scoped provider that receives the controlled headless window dimensions. */
    val screenSizeProvider: DefaultScreenSizeProvider

    /** Factory for creating the production template provider with a deterministic test scope. */
    val templateProviderInternalFactory: TemplateProvider.InternalFactory
  }
}
