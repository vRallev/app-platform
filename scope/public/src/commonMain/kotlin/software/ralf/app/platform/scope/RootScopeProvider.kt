package software.ralf.app.platform.scope

/**
 * Provides the root scope of the application, which usually refers to `AppScope`. This interface is
 * implemented by the `Application` class of the app.
 */
public interface RootScopeProvider {
  /**
   * The root scope of the application, which usually refers to `AppScope`. Usually, the scope is
   * alive until the application terminates.
   */
  public val rootScope: Scope
}
