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
 * Returns the module type inferred from an absolute Gradle project path such as `:feature:public`.
 *
 * Explicit module types in the final path segment take precedence over app-like parent segments.
 * Returns [ModuleType.UNKNOWN] when no supported module naming convention matches. The receiver is
 * not otherwise validated as a Gradle project path.
 */
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
