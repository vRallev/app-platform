package software.ralf.app.platform.listdetail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin
import software.ralf.app.platform.gradle.AppPlatformExtension

internal class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.version = target.versionName

    target.tasks.register("release")
    target.configureAppPlatform()
    target.configureTestLogging()
    target.upgradeYarnDependencies()
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

  private fun Project.upgradeYarnDependencies() {
    plugins.withType(CommonYarnPlugin::class.java).configureEach {
      with(extensions.getByType(BaseYarnRootExtension::class.java)) {
        resolution("webpack-dev-server", "5.2.5")
        resolution("fast-uri", "3.1.2")
        resolution("picomatch", "2.3.2")
        resolution("path-to-regexp", "0.1.13")
        resolution("ws", "8.21.0")
        resolution("ajv", "8.20.0")
        resolution("qs", "6.15.2")
        resolution("http-proxy-middleware", "2.0.10")
        resolution("uuid", "11.1.1")
      }
    }
  }
}
