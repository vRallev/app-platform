package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/** Metro graph that is used in UI tests. */
@DependencyGraph(AppScope::class)
interface TestDesktopAppGraph : DesktopApp.Graph {
  /** Factory for a desktop UI test graph. */
  @DependencyGraph.Factory
  fun interface Factory {
    /** Creates a graph using the test application's [rootScopeProvider]. */
    fun create(@Provides rootScopeProvider: RootScopeProvider): TestDesktopAppGraph
  }
}
