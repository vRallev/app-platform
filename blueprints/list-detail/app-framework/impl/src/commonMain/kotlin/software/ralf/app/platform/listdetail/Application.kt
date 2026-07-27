package software.ralf.app.platform.listdetail

import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph
import software.ralf.app.platform.scope.register

/** Shared application lifecycle used by every platform entry point. */
class Application : RootScopeProvider {
  private var rootScopeOrNull: Scope? = null

  override val rootScope: Scope
    get() = checkNotNull(rootScopeOrNull) { "Must call create() first." }

  /** Creates and retains the root scope. */
  fun create(appGraph: AppGraph) {
    check(rootScopeOrNull == null) { "create() should be called only once." }

    rootScopeOrNull = Scope.buildRootScope {
      addMetroDependencyGraph(appGraph)
      addCoroutineScopeScoped(appGraph.appScopeCoroutineScopeScoped)
    }

    // Register after assigning rootScopeOrNull because a Scoped instance can access the root scope.
    rootScope.register(appGraph.appScopedInstances)
  }

  /** Destroys the root scope and releases its scoped objects. */
  fun destroy() {
    rootScope.destroy()
    rootScopeOrNull = null
  }
}
