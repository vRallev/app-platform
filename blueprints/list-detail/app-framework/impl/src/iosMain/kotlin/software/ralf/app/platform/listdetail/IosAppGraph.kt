package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIApplication
import software.ralf.app.platform.scope.RootScopeProvider

/** Final iOS graph, including dependencies supplied by UIKit. */
@DependencyGraph(AppScope::class)
interface IosAppGraph {
  /** Factory for the iOS application graph. */
  @DependencyGraph.Factory
  fun interface Factory {
    /** Creates a graph with UIKit and root-scope dependencies. */
    fun create(
      @Provides uiApplication: UIApplication,
      @Provides rootScopeProvider: RootScopeProvider,
    ): IosAppGraph
  }
}

/** Called from Swift to create the production iOS graph. */
@Suppress("unused")
fun createIosAppGraph(application: UIApplication, rootScopeProvider: RootScopeProvider): AppGraph {
  return createGraphFactory<IosAppGraph.Factory>().create(application, rootScopeProvider)
}
