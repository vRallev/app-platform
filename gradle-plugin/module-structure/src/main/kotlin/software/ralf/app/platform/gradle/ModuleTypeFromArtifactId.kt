package software.ralf.app.platform.gradle

import software.ralf.app.platform.gradle.ModuleType.APP
import software.ralf.app.platform.gradle.ModuleType.IMPL
import software.ralf.app.platform.gradle.ModuleType.IMPL_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.INTERNAL
import software.ralf.app.platform.gradle.ModuleType.INTERNAL_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.PUBLIC
import software.ralf.app.platform.gradle.ModuleType.PUBLIC_ROBOTS
import software.ralf.app.platform.gradle.ModuleType.TESTING
import software.ralf.app.platform.gradle.ModuleType.UNKNOWN

/**
 * Returns the module type inferred from a Maven artifact ID such as `feature-public` or
 * `feature-impl-debug-robots`.
 *
 * Library suffixes take precedence over the `app-` prefix. Returns [ModuleType.UNKNOWN] when no
 * supported naming convention matches.
 */
public fun String.moduleTypeFromArtifactId(): ModuleType {
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
