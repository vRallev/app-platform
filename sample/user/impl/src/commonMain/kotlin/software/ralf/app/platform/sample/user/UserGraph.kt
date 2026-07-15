package software.ralf.app.platform.sample.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped
import software.ralf.app.platform.scope.coroutine.IoCoroutineDispatcher

/** The Metro graph for the user scope. This is a graph extension of the AppScope graph. */
@GraphExtension(UserScope::class)
interface UserGraph {
  /**
   * The factory instantiates a new instance of [UserGraph]. This interface will be implemented by
   * the AppScope graph.
   */
  @GraphExtension.Factory
  @ContributesTo(AppScope::class)
  interface Factory {
    /**
     * Creates a new instance of [UserGraph]. The provided [user] argument will be added to the
     * graph and the [User] can be injected in the classes part of the [UserScope].
     */
    fun createUserGraph(@Provides user: User): UserGraph
  }

  /** All [Scoped] instances part of the user scope. */
  @ForScope(UserScope::class) @Multibinds(allowEmpty = true) val userScopedInstances: Set<Scoped>

  /** The coroutine scope that runs as long as the user scope is alive. */
  @ForScope(UserScope::class) val userScopeCoroutineScopeScoped: CoroutineScopeScoped

  /**
   * Provides the [CoroutineScopeScoped] for the user scope. This is a single instance for the user
   * scope.
   */
  @Provides
  @SingleIn(UserScope::class)
  @ForScope(UserScope::class)
  fun provideUserScopeCoroutineScopeScoped(
    @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
  ): CoroutineScopeScoped {
    return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("UserScope"))
  }

  /**
   * Provides the [CoroutineScope] for the user scope. A new child scope is created every time an
   * instance is injected so that the parent cannot be canceled accidentally.
   */
  @Provides
  @ForScope(UserScope::class)
  fun provideUserCoroutineScope(
    @ForScope(UserScope::class) userScopeCoroutineScopeScoped: CoroutineScopeScoped
  ): CoroutineScope {
    return userScopeCoroutineScopeScoped.createChild()
  }
}
