@file:Suppress("TooManyFunctions", "unused")

package software.ralf.app.platform.gradle

import org.gradle.api.Project
import software.ralf.app.platform.gradle.ModuleType.APP
import software.ralf.app.platform.gradle.ModuleType.IMPL
import software.ralf.app.platform.gradle.ModuleType.IMPL_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.INTERNAL
import software.ralf.app.platform.gradle.ModuleType.INTERNAL_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.PUBLIC
import software.ralf.app.platform.gradle.ModuleType.PUBLIC_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.TESTING
import software.ralf.app.platform.gradle.ModuleType.UNKNOWN

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

/** The type of module based on our module structure. */
public val Project.moduleType: ModuleType
  get() = path.moduleTypeFromProjectPath()

/** Returns the module type inferred from this Gradle project path. */
public fun String.moduleTypeFromProjectPath(): ModuleType {
  val name = substringAfterLast(':')

  val isRobots = name.endsWith("-robots")

  return when {
    name.startsWith("public") -> if (isRobots) PUBLIC_ROBOTS else PUBLIC
    name == "testing" -> TESTING
    name.startsWith("impl") -> if (isRobots) IMPL_ROBOTS else IMPL
    name.startsWith("internal") -> if (isRobots) INTERNAL_ROBOTS else INTERNAL
    isAppModulePath() -> APP
    else -> UNKNOWN
  }
}

private fun String.isAppModulePath(): Boolean {
  val name = substringAfterLast(':')

  return name.isAppSegment() ||
    contains(":app:") ||
    contains(":apps:") ||
    contains(":app-") ||
    contains(":apps-")
}

private fun String.isAppSegment(): Boolean =
  this == "app" || this == "apps" || startsWith("app-") || startsWith("apps-")

internal fun String.moduleTypeFromArtifactId(): ModuleType {
  // E.g. abc-public, def-impl-xyz-robots
  return when {
    endsWith("-public-robots") -> PUBLIC_ROBOTS
    endsWith("-public") -> PUBLIC
    endsWith("-testing") -> TESTING
    endsWith("-impl") -> IMPL
    contains("-impl-") -> if (endsWith("-robots")) IMPL_ROBOTS else IMPL
    endsWith("-internal") -> INTERNAL
    contains("-internal-") -> if (endsWith("-robots")) INTERNAL_ROBOTS else INTERNAL
    this == "app" -> APP
    startsWith("app-") -> APP
    else -> UNKNOWN
  }
}

/**
 * Returns true for app modules. Typically, these modules are leaf modules prefixed with "app" or
 * live in a folder named "app".
 */
public fun Project.isAppModule(): Boolean = moduleType == APP

/** Returns true for any public module including robots module. */
public fun Project.isAnyPublicModule(): Boolean =
  moduleType == PUBLIC || moduleType == PUBLIC_ROBOTS

/** Returns true for the public module of a library, but not subtypes, e.g. a robots module. */
public fun Project.isPublicModule(): Boolean = moduleType == PUBLIC

/** Returns true for the testing module of a library. */
public fun Project.isTestingModule(): Boolean = moduleType == TESTING

/** Returns true for any impl module including robots module. */
public fun Project.isAnyImplModule(): Boolean = moduleType == IMPL || moduleType == IMPL_ROBOTS

/** Returns true for an impl module, but not subtypes, e.g. a robots module. */
public fun Project.isImplModule(): Boolean = moduleType == IMPL

/** Returns true for an internal module, but not subtypes, e.g. a robots module. */
public fun Project.isAnyInternalModule(): Boolean =
  moduleType == INTERNAL || moduleType == INTERNAL_ROBOTS

/** Returns true for an internal module, but not subtypes, e.g. a robots module. */
public fun Project.isInternalModule(): Boolean = moduleType == INTERNAL

/** Returns true for any robots module. */
public fun Project.isRobotsModule(): Boolean = moduleType.isRobotsModule

/** Checks whether the project follows the naming convention of the module structure. */
public fun Project.isUsingModuleStructure(): Boolean = moduleType != UNKNOWN
