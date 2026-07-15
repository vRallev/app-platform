package software.ralf.app.platform.robot.internal

import software.ralf.app.platform.scope.RootScopeProvider

/** Not intended for common usage. */
public object RobotInternals {

  /**
   * Allows you to override the root scope used to initialize test robots. Some platforms, like
   * Android, try to get the root scope automatically and this function doesn't need to be called.
   * Other platforms like Desktop must set the [RootScopeProvider] before a test runs.
   */
  public fun setRootScopeProvider(rootScopeProvider: RootScopeProvider?) {
    software.ralf.app.platform.robot.internal.rootScopeProvider = rootScopeProvider
  }
}
