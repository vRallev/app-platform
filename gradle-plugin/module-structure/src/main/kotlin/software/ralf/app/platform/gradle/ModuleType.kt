package software.ralf.app.platform.gradle

/** The type of module based on our module structure. */
public enum class ModuleType(
  /** Whether this type is a robots module. Robot modules are used for instrumented tests. */
  public val isRobotsModule: Boolean = false,

  /**
   * Whether dependencies that typically used only in tests are part of the main source set, e.g.
   * that's the case for `:testing` and robot modules.
   */
  public val useTestDependenciesInMain: Boolean = false,
) {
  /**
   * `:app` modules refer to the final application, where all feature implementations are imported
   * and assembled as a single binary. Therefore, `:app` modules are allowed to depend on `:impl`
   * modules of all imported libraries and features.
   *
   * App modules are leaf modules prefixed with "app" or live in a folder named "app".
   */
  APP,

  /**
   * `:public` modules contain the code that should be shared and reused by other modules and
   * libraries. APIs (interfaces) usually live in `:public` modules, but also code where dependency
   * inversion isn’t applied such as static utilities, extension functions and UI components.
   */
  PUBLIC,

  /** `:public-robots` host robots or test code for a `:public` module. */
  PUBLIC_ROBOTS(isRobotsModule = true, useTestDependenciesInMain = true),

  /**
   * `:testing` modules provide a mechanism to share utilities or fake implementations for tests
   * with other libraries. `:testing` modules are allowed to be imported as test dependency by any
   * other module type and are never added to the runtime classpath. Even its own `:public` module
   * can reuse the code from the `:testing` module for its tests.
   */
  TESTING(useTestDependenciesInMain = true),

  /**
   * `:impl` modules contain the concrete implementations of the API from `:public` modules. A
   * library can have zero or more `:impl` modules. If a library contains multiple `:impl` modules,
   * then they’re suffixed, e.g. `:login:impl-amazon` and `:login:impl-google`.
   */
  IMPL,

  /**
   * `:*-robots` modules help implementing the robot pattern for UI tests and make them shareable.
   * Robots must know about concrete implementations, therefore they usually depend on an `:impl`
   * module, but don't expose this `:impl` module on the compile classpath. `:robot` modules are
   * only imported and reused for UI tests and are never added as dependency to the runtime
   * classpath of a module similar to `:testing` modules.
   */
  IMPL_ROBOTS(isRobotsModule = true, useTestDependenciesInMain = true),

  /**
   * `:internal` modules are used when code should be shared between multiple `:impl` modules of the
   * same library, but the code should not be exposed through the `:public` module. This code is
   * "internal" to this library.
   */
  INTERNAL,

  /** `:internal-robots` host robots or test code for an `:internal` module. */
  INTERNAL_ROBOTS(isRobotsModule = true, useTestDependenciesInMain = true),

  /**
   * The module type could not be parsed, likely because the module is not following the module
   * structure.
   */
  UNKNOWN,
}
