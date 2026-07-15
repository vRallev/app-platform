package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * The final Wasm app graph.
 *
 * Unlike the Android and iOS specific counterpart, this class doesn't have any platform specific
 * types.
 */
@DependencyGraph(AppScope::class)
interface WasmJsAppGraph {
  /** The factory to create a new instance of [WasmJsAppGraph]. */
  @DependencyGraph.Factory
  fun interface Factory {
    /**
     * Creates a new [WasmJsAppGraph] instance. [[rootScopeProvider] is provided in the
     * [WasmJsAppGraph] and can be injected.
     */
    fun create(@Provides rootScopeProvider: RootScopeProvider): WasmJsAppGraph
  }

  /** Gives access to the [TemplateProvider.Factory] from the object graph. */
  val templateProviderFactory: TemplateProvider.Factory
}
