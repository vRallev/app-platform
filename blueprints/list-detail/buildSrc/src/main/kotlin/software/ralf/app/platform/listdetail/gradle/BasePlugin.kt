package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import software.ralf.app.platform.gradle.AppPlatformExtension

internal class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.version = target.versionName

    target.tasks.register("release")
    target.configureAppPlatform()
    target.configureTestLogging()
  }

  private fun Project.configureAppPlatform() {
    if (path == ":") return

    plugins.apply(Plugins.APP_PLATFORM)
    extensions.getByType(AppPlatformExtension::class.java).enableModuleStructure(true)
    releaseTask.configure { it.dependsOn("checkModuleStructureDependencies") }
  }

  private fun Project.configureTestLogging() {
    tasks.withType(Test::class.java).configureEach {
      it.systemProperty("java.awt.headless", "true")

      if (ci) {
        it.testLogging { logging ->
          logging.showExceptions = true
          logging.showCauses = true
          logging.showStackTraces = true
          logging.showStandardStreams = true
        }
      }
    }
  }
}
