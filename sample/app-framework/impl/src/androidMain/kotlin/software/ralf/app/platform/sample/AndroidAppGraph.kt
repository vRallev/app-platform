package software.ralf.app.platform.sample

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * The final Android app graph.
 *
 * Note that [Application] is an Android specific type and classes living in the Android source
 * folder can therefore inject [Application].
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph {
  /** The factory to create a new instance of [AndroidAppGraph]. */
  @DependencyGraph.Factory
  fun interface Factory {
    /**
     * Creates a new [AndroidAppGraph] instance. [application] and [rootScopeProvider] are provided
     * in the [AndroidAppGraph] and can be injected.
     */
    fun create(
      @Provides application: Application,
      @Provides rootScopeProvider: RootScopeProvider,
    ): AndroidAppGraph
  }
}
