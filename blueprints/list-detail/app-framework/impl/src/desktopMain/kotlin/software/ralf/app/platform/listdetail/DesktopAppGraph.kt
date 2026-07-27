package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/** Final Desktop application graph. */
@DependencyGraph(AppScope::class)
interface DesktopAppGraph {
  /** Factory for the Desktop application graph. */
  @DependencyGraph.Factory
  fun interface Factory {
    /** Creates a graph with the shared root-scope provider. */
    fun create(@Provides rootScopeProvider: RootScopeProvider): DesktopAppGraph
  }
}
