package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIApplication
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * The final iOS app graph.
 *
 * Note that [UIApplication] is an iOS specific type and classes living in the iOS source folder can
 * therefore inject [UIApplication].
 */
@DependencyGraph(AppScope::class)
interface IosAppGraph {
  /** The factory to create a new instance of [IosAppGraph]. */
  @DependencyGraph.Factory
  fun interface Factory {
    /**
     * Creates a new [IosAppGraph] instance. [uiApplication] and [rootScopeProvider] are provided in
     * the [IosAppGraph] and can be injected.
     */
    fun create(
      @Provides uiApplication: UIApplication,
      @Provides rootScopeProvider: RootScopeProvider,
    ): IosAppGraph
  }

  /** Gives access to the [TemplateProvider.Factory] from the object graph. */
  val templateProviderFactory: TemplateProvider.Factory
}

/** This function is called from Swift to create a new graph instance. */
@Suppress("unused")
fun createIosAppGraph(application: UIApplication, rootScopeProvider: RootScopeProvider): AppGraph {
  return createGraphFactory<IosAppGraph.Factory>().create(application, rootScopeProvider)
}
