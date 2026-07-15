package software.ralf.app.platform.scope.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** Component providing coroutine scopes in the App scope. */
@ContributesTo(AppScope::class)
public interface AppScopeCoroutineScopeComponent {
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
