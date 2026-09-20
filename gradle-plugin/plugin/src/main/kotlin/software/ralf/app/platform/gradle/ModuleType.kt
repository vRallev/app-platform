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
public val Project.moduleType: ModuleType
  get() = path.moduleTypeFromProjectPath()

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
