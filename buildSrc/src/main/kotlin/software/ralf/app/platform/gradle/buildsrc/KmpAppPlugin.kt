package software.ralf.app.platform.gradle.buildsrc

import kotlin.math.max
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import software.ralf.app.platform.gradle.AppPlatformPlugin
import software.ralf.app.platform.gradle.buildsrc.App.Companion.app
import software.ralf.app.platform.gradle.buildsrc.App.Companion.appOrNull
import software.ralf.app.platform.gradle.buildsrc.KmpPlugin.Companion.composeMultiplatform
import software.ralf.app.platform.gradle.buildsrc.KmpPlugin.Companion.kmpExtension
import software.ralf.app.platform.gradle.isAppModule
import software.ralf.app.platform.gradle.isRobotsModule
import software.ralf.app.platform.gradle.isTestingModule

public open class KmpAppPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)

    val app = target.app
    when (target.path) {
      app.appFrameworkProjectPath -> target.plugins.apply(KmpAndroidPlugin::class.java)
      app.desktopAppProjectPath,
      app.webAppProjectPath -> Unit
      else -> error("The KMP app plugin cannot be applied to ${target.path}.")
    }

    target.plugins.apply(KmpPlugin::class.java)

    when (target.path) {
      app.appFrameworkProjectPath -> target.addDependencies()
      app.desktopAppProjectPath ->
        target.plugins.withId(Plugins.COMPOSE_MULTIPLATFORM) { target.configureDesktopApp() }
      app.webAppProjectPath -> target.configureWasm()
    }
  }

  private fun Project.addDependencies() {
    // iOS exports these dependencies for the iOS Framework and requires them to be added as
    // "api" dependency to the project.
    allExportedDependencies().forEach { dependency ->
      kmpExtension.sourceSets.getByName("commonMain").dependencies { api(dependency) }
    }
  }

  @OptIn(ExperimentalWasmDsl::class)
  private fun Project.configureWasm() {
    // For development use the Gradle task 'wasmJsBrowserDevelopmentRun'.
    //
    // Release builds are built with 'wasmJsBrowserDistribution'. To test the release run
    // 'npx http-server' from the folder 'sample/app/web/build/dist/wasmJs/productionExecutable'.

    // Keep references to the Project outside of the lambdas below, otherwise this will break
    // the configuration cache.
    val jsFileName = app.jsFileName
    val outputName = safePathString

    kmpExtension.wasmJs {
      browser {
        outputModuleName.set(outputName)
        commonWebpackConfig {
          it.outputFileName = jsFileName
          it.devServer = it.devServer ?: KotlinWebpackConfig.DevServer()
        }
      }
      binaries.executable()
    }
  }
}

internal fun Project.allExportedDependencies(): Set<Any> {
  return AppPlatformPlugin.exportedDependencies()
    .plus(
      project(app.rootProjectPath)
        .subprojects
        .filter { it.subprojects.isEmpty() }
        .filter { it.appOrNull == null }
        .filter { !it.isRobotsModule() && !it.isTestingModule() && !it.isAppModule() }
    )
}

private fun Project.configureDesktopApp() {
  composeMultiplatform.extensions.getByType(DesktopExtension::class.java).application.apply {
    mainClass = app.desktopMainFile

    nativeDistributions.targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
    nativeDistributions.packageName = "software.ralf.app.platform.demo"

    // During development the major version is 0, e.g. '0.0.1'. DMG must use a
    // major version equal or greater than 1:
    //
    // Illegal version for 'Dmg': '0.0.1' is not a valid build version.
    val version = VersionNumber.parse(versionName)
    nativeDistributions.packageVersion =
      VersionNumber(max(1, version.major), version.minor, version.patch, null).toString()
  }
}
