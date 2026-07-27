package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * Desktop integration-test graph that includes production bindings and contributed test robots.
 *
 * Keeping this graph in the robot fixture module lets Metro discover every test robot without
 * adding robot dependencies to the production desktop application.
 */
@DependencyGraph(AppScope::class)
interface TestDesktopAppGraph {
  /** Factory for a Desktop test graph. */
  @DependencyGraph.Factory
  interface Factory {
    /** Creates a graph using the test application's root-scope provider. */
    fun create(@Provides rootScopeProvider: RootScopeProvider): TestDesktopAppGraph
  }
}
