package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Multibinds
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped

/**
 * Shared interface for the app graph. The final graphs live in platform-specific source sets so
 * they can access platform-specific code.
 */
@ContributesTo(AppScope::class)
interface AppGraph {
  /** All [Scoped] instances part of the app scope. */
  @ForScope(AppScope::class) @Multibinds(allowEmpty = true) val appScopedInstances: Set<Scoped>

  /** The coroutine scope that runs as long as the app scope is alive. */
  @ForScope(AppScope::class) val appScopeCoroutineScopeScoped: CoroutineScopeScoped
}
