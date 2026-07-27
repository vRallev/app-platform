package software.ralf.app.platform.listdetail

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/** Final Android graph, including dependencies supplied by the Android application. */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph {
  /** Factory for the Android application graph. */
  @DependencyGraph.Factory
  fun interface Factory {
    /** Creates a graph with Android and root-scope dependencies. */
    fun create(
      @Provides application: Application,
      @Provides rootScopeProvider: RootScopeProvider,
    ): AndroidAppGraph
  }
}
