package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * The final Desktop app graph. Unlike the Android and iOS specific counterpart, this class doesn't
 * have any platform specific types.
 */
@DependencyGraph(AppScope::class)
interface DesktopAppGraph {
  /** The factory to create a new instance of [DesktopAppGraph]. */
  @DependencyGraph.Factory
  fun interface Factory {
    /**
     * Creates a new [DesktopAppGraph] instance. [rootScopeProvider] is provided in the
     * [DesktopAppGraph] and can be injected.
     */
    fun create(@Provides rootScopeProvider: RootScopeProvider): DesktopAppGraph
  }
}
