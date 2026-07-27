package software.ralf.app.platform.listdetail

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * Android integration-test graph that includes production bindings and contributed test robots.
 *
 * The graph is compiled in the robot fixture module so Metro can discover robots that are absent
 * from the production application classpath.
 */
@DependencyGraph(AppScope::class)
interface TestAndroidAppGraph {
  /** Factory for an Android test graph. */
  @DependencyGraph.Factory
  interface Factory {
    /** Creates a graph using the test application and its root-scope provider. */
    fun create(
      @Provides application: Application,
      @Provides rootScopeProvider: RootScopeProvider,
    ): TestAndroidAppGraph
  }
}
