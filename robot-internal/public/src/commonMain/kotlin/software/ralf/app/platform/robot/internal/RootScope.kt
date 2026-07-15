package software.ralf.app.platform.robot.internal

import software.ralf.app.platform.scope.Scope

/**
 * A static instance of the root scope. Robots use this as default instance to initialize
 * themselves.
 */
public val rootScope: Scope
  get() =
    checkNotNull(rootScopeProvider?.rootScope ?: defaultRootScope) {
      "The root scope could not be found. Consider overriding the RootScopeProvider " +
        "through RobotInternals."
    }
