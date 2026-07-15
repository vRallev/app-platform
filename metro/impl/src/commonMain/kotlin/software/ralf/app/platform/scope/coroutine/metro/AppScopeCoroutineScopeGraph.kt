package software.ralf.app.platform.scope.coroutine.metro

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped
import software.ralf.app.platform.scope.coroutine.IoCoroutineDispatcher

/** Graph providing coroutine scopes in the App scope. */
@ContributesTo(AppScope::class)
@BindingContainer
public object AppScopeCoroutineScopeGraph {
  /**
   * Provides the [CoroutineScopeScoped] for the app scope. This is a single instance for the app
   * scope.
   */
  @Provides
  @SingleIn(AppScope::class)
  @ForScope(AppScope::class)
  public fun provideAppScopeCoroutineScopeScoped(
    @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
  ): CoroutineScopeScoped {
    return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("AppScope"))
  }

  /**
   * Provides the [CoroutineScope] for the app scope. A new child scope is created every time an
   * instance is injected so that the parent cannot be canceled accidentally.
   */
  @Provides
  @ForScope(AppScope::class)
  public fun provideAppCoroutineScope(
    @ForScope(AppScope::class) appScopeCoroutineScopeScoped: CoroutineScopeScoped
  ): CoroutineScope {
    return appScopeCoroutineScopeScoped.createChild()
  }
}
