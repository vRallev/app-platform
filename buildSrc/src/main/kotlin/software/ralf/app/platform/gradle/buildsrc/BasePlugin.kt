package software.ralf.app.platform.gradle.buildsrc

import buildSrc.BuildConfig.APP_PLATFORM_GROUP
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformGradlePlugin

public open class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.createReleaseTask()
    target.configureDependencySubstitution()

    // We're dogfooding our published Gradle plugin in the :app module. The extension names
    // are conflicting, therefore use another name than "appPlatform".
    target.extensions.create("appPlatformBuildSrc", AppPlatformExtension::class.java)

    target.addAppPlatformGradlePlugin()
    target.runTestsInHeadlessMode()
    target.configureLogOutput()
    target.upgradeYarnDependencies()
  }

  private fun Project.createReleaseTask() {
    tasks.register("release")
  }

  private fun Project.runTestsInHeadlessMode() {
    // Otherwise the java icon keeps popping up in the system tray while running tests.
    tasks.withType(Test::class.java).configureEach {
      it.systemProperty("java.awt.headless", "true")
    }
  }

  private fun Project.configureLogOutput() {
    if (ci) {
      tasks.withType(Test::class.java).configureEach { testTask ->
        testTask.testLogging {
          it.showExceptions = true
          it.showCauses = true
          it.showStackTraces = true
          it.showStandardStreams = true
        }
      }
    }
  }

  private fun Project.configureDependencySubstitution() {
    // In some modules we apply the App Platform Gradle plugin, which adds dependencies to
    // these pre-built binaries. Here we tell Gradle to replace the pre-built binaries with
    // the Gradle modules and build the code on the fly. See settings.gradle for more details
    // as well.
    val substitutions =
      mapOf(
        "${APP_PLATFORM_GROUP}:common-public" to ":common:public",
        "${APP_PLATFORM_GROUP}:di-common-public" to ":di-common:public",
        "${APP_PLATFORM_GROUP}:kotlin-inject-public" to ":kotlin-inject:public",
        "${APP_PLATFORM_GROUP}:kotlin-inject-contribute-impl-code-generators" to
          ":kotlin-inject-extensions:contribute:impl-code-generators",
        "${APP_PLATFORM_GROUP}:kotlin-inject-contribute-public" to
          ":kotlin-inject-extensions:contribute:public",
        "${APP_PLATFORM_GROUP}:kotlin-inject-impl" to ":kotlin-inject:impl",
        "${APP_PLATFORM_GROUP}:ksp-common-public" to ":ksp-common:public",
        "${APP_PLATFORM_GROUP}:metro-public" to ":metro:public",
        "${APP_PLATFORM_GROUP}:metro-impl" to ":metro:impl",
        "${APP_PLATFORM_GROUP}:metro-contribute-impl-compiler-plugin" to
          ":metro-extensions:contribute:impl-compiler-plugin",
        "${APP_PLATFORM_GROUP}:metro-contribute-impl-code-generators" to
          ":metro-extensions:contribute:impl-code-generators",
        "${APP_PLATFORM_GROUP}:presenter-backstack-nav3-public" to
          ":presenter-backstack-nav3:public",
        "${APP_PLATFORM_GROUP}:presenter-backstack-nav3-testing" to
          ":presenter-backstack-nav3:testing",
        "${APP_PLATFORM_GROUP}:presenter-public" to ":presenter:public",
        "${APP_PLATFORM_GROUP}:presenter-molecule-public" to ":presenter-molecule:public",
        "${APP_PLATFORM_GROUP}:presenter-molecule-impl" to ":presenter-molecule:impl",
        "${APP_PLATFORM_GROUP}:presenter-molecule-testing" to ":presenter-molecule:testing",
        "${APP_PLATFORM_GROUP}:renderer-public" to ":renderer:public",
        "${APP_PLATFORM_GROUP}:renderer-android-view-public" to ":renderer-android-view:public",
        "${APP_PLATFORM_GROUP}:renderer-compose-multiplatform-public" to
          ":renderer-compose-multiplatform:public",
        "${APP_PLATFORM_GROUP}:robot-public" to ":robot:public",
        "${APP_PLATFORM_GROUP}:robot-compose-multiplatform-public" to
          ":robot-compose-multiplatform:public",
        "${APP_PLATFORM_GROUP}:robot-internal-public" to ":robot-internal:public",
        "${APP_PLATFORM_GROUP}:scope-public" to ":scope:public",
        "${APP_PLATFORM_GROUP}:scope-testing" to ":scope:testing",
      )

    plugins.withId(Plugins.MAVEN_PUBLISH) {
      check(path in substitutions.values) {
        "Forgot to setup dependency substitution for $path. Add a mapping in the " +
          "substitution collection."
      }
    }

    configurations.configureEach { configuration ->
      configuration.resolutionStrategy.dependencySubstitution { substitution ->
        substitutions.forEach { (module, project) ->
          substitution.substitute(substitution.module(module)).using(substitution.project(project))
        }
      }
    }
  }

  private fun Project.addAppPlatformGradlePlugin() {
    if (!isRoot) {
      plugins.apply(Plugins.APP_PLATFORM)

      plugins.withIds(Plugins.KOTLIN_MULTIPLATFORM, Plugins.KOTLIN_JVM) {
        appPlatformGradlePlugin.enableModuleStructure(true)

        releaseTask.dependsOn("checkModuleStructureDependencies")
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
