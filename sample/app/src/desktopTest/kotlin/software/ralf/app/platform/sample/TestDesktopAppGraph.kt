package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/** Metro graph that is used in UI tests. */
@DependencyGraph(AppScope::class)
interface TestDesktopAppGraph : DesktopApp.Graph {
  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides rootScopeProvider: RootScopeProvider): TestDesktopAppGraph
  }
}
