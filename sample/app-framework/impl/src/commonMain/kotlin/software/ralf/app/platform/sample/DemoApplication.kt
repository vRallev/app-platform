package software.ralf.app.platform.sample

import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph
import software.ralf.app.platform.scope.register

/**
 * Shared class between the platform to manage the root scope. It itself implements the
 * [RootScopeProvider] interface.
 */
class DemoApplication : RootScopeProvider {

  private var _rootScope: Scope? = null

  override val rootScope: Scope
    get() = checkNotNull(_rootScope) { "Must call create() first." }

  /** Creates the root scope and remembers the instance. */
  fun create(appGraph: AppGraph) {
    check(_rootScope == null) { "create() should be called only once." }

    _rootScope = Scope.buildRootScope {
      addMetroDependencyGraph(appGraph)

      addCoroutineScopeScoped(appGraph.appScopeCoroutineScopeScoped)
    }

    // Register instances after the rootScope has been set to avoid race conditions for Scoped
    // instances that may use the rootScope.
    rootScope.register(appGraph.appScopedInstances)
  }

  /** Destroys the root scope. */
  fun destroy() {
    rootScope.destroy()
    _rootScope = null
  }
}
