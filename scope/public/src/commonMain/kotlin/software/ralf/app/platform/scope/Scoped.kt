package software.ralf.app.platform.scope

/**
 * Register this interface with a [Scope] to receive lifecycle callbacks of this scope.
 *
 * This interface is commonly used with service objects to receive a notification when a [Scope] is
 * created and when the [Scope] is destroyed to clean up resources, e.g.
 *
 * ```
 * interface HudManager {
 *     ...
 * }
 *
 * @Inject
 * @SingleIn(AppScope::class)
 * class HudManagerImpl(
 *     ...
 * ) : HudManager, Scoped {
 *     ...
 *
 *     override fun onEnterScope(scope: Scope) {
 *         // Initialize code and start background work
 *     }
 *
 *     override fun onExitScope() {
 *         // Free up resources
 *     }
 *
 *     interface Component {
 *         @Provides
 *         fun provideHudManager(impl: HudManagerImpl): HudManager = impl
 *
 *         @Provides
 *         @IntoSet
 *         fun provideHudManagerScoped(impl: HudManagerImpl): AppScopeScoped = impl
 *   }
 * }
 * ```
 *
 * In this particular example `HudManagerImpl` gets automatically created by our DI framework, when
 * the app scope is created. The same mechanism works for other scopes. Other consumers can safely
 * inject `HudManager` and would receive an instance of `HudManagerImpl`.
 *
 * It's an anti-pattern to make interfaces like `HudManager` in this example extend the [Scoped]
 * interface. Implementing [Scoped] is a pure implementation detail.
 *
 * If the application crashes, then [onExitScope] will not be called for any scope. Upon relaunch
 * it's up to the application to restore any scope and invoke [onEnterScope] again.
 */
public interface Scoped {
  /**
   * This function is called when the given [scope] is created or was already created by the time
   * this [Scoped] is being registered.
   *
   * This method provides no guarantee on which thread it is invoked. It can be the main thread or a
   * background thread. E.g. for the `AppScope` it's usually invoked on the main thread, when the
   * application starts, but other service classes hosting more fine grained scopes may use a
   * background thread.
   */
  public fun onEnterScope(scope: Scope): Unit = Unit

  /**
   * Called when the scope is destroyed. This callback should be used to clean up resources and stop
   * ongoing work. This function is a blocking / non-suspending call and should not start any
   * background work, which may lead to race conditions. The `CoroutineScope` attached to this scope
   * is already canceled by the time this function runs.
   *
   * This method provides no guarantee on which thread it is invoked. It can be the main thread or a
   * background thread. E.g. for the `AppScope` it's usually invoked on the main thread, when the
   * application is destroyed, but other service classes hosting more fine grained scopes may use a
   * background thread.
   */
  public fun onExitScope(): Unit = Unit

  public companion object {
    /** A [Scoped] implementation that does nothing in the lifecycle methods. */
    public val NO_OP: Scoped = object : Scoped {}
  }
}
