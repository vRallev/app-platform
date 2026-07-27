package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Multibinds
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped

/**
 * Shared interface for the application graph. Final graphs live in platform source sets so they can
 * provide platform-specific dependencies.
 */
@ContributesTo(AppScope::class)
interface AppGraph {
  /** All [Scoped] instances that share the application lifecycle. */
  @ForScope(AppScope::class) @Multibinds(allowEmpty = true) val appScopedInstances: Set<Scoped>

  /** Coroutine scope that lives as long as the application scope. */
  @ForScope(AppScope::class) val appScopeCoroutineScopeScoped: CoroutineScopeScoped
}
