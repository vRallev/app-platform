package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Project

internal enum class App(val rootProjectPath: String) {
  RECIPES(":recipes"),
  SAMPLE(":sample");

  val appFrameworkProjectPath: String = "$rootProjectPath:app-framework:impl"
  val androidAppProjectPath: String = "$rootProjectPath:app:android"
  val desktopAppProjectPath: String = "$rootProjectPath:app:desktop"
  val webAppProjectPath: String = "$rootProjectPath:app:web"

  val iosFrameworkName: String = rootProjectPath.substring(1).capitalize() + "App"
  val jsFileName: String = rootProjectPath.substring(1) + "-app.js"
  val desktopMainFile: String = "software.ralf.app.platform.${rootProjectPath.substring(1)}.MainKt"

  private fun containsProjectPath(projectPath: String): Boolean {
    return projectPath == appFrameworkProjectPath ||
      projectPath == androidAppProjectPath ||
      projectPath == desktopAppProjectPath ||
      projectPath == webAppProjectPath
  }

  companion object {
    val Project.appOrNull: App?
      get() = entries.singleOrNull { it.containsProjectPath(path) }

    val Project.app: App
      get() = checkNotNull(appOrNull) { "Project $path is not an application module." }
  }
}
