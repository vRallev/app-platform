package software.amazon.app.platform.gradle.buildsrc

import kotlin.math.max
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import software.amazon.app.platform.gradle.AppPlatformPlugin
import software.amazon.app.platform.gradle.buildsrc.AppPlugin.App.Companion.app
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.composeMultiplatform
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.kmpExtension
import software.amazon.app.platform.gradle.isAppModule
import software.amazon.app.platform.gradle.isRobotsModule
import software.amazon.app.platform.gradle.isTestingModule

public open class AppPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.ANDROID_APP)

    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
    target.plugins.apply(BaseAndroidPlugin::class.java)

    target.configureAndroidSettings()
    target.makeSingleVariant()
    target.addDependencies()
    target.addDependencyConstraints()
    target.configureWasm()

    target.plugins.withId(Plugins.COMPOSE_MULTIPLATFORM) { target.configureDesktopApp() }
  }

  private fun Project.configureAndroidSettings() {
    android.defaultConfig.minSdk = 26
  }

  private fun Project.makeSingleVariant() {
    // Disable the release build type in the app module. We only need one build type
    // and everything else is overhead.
    androidComponents.beforeVariants { variant ->
      if (variant.buildType != "debug") {
        variant.enable = false
      }
    }
  }

  private fun Project.addDependencies() {
    // iOS exports these dependencies for the iOS Framework and requires them to be added as
    // "api" dependency to the project.
    allExportedDependencies().forEach { dependency ->
      kmpExtension.sourceSets.getByName("commonMain").dependencies { api(dependency) }
    }
  }

  private fun Project.addDependencyConstraints() {
    // This is needed for Lint in app modules:
    //
    // Could not determine the dependencies of task
    // ':recipes:app:generateDebugAndroidTestLintModel'.
    //  > Could not resolve all dependencies for configuration
    // ':recipes:app:debugAndroidTestRuntimeClasspath'.
    //   > Could not resolve androidx.concurrent:concurrent-futures:1.2.0.
    //
    // TODO: Remove when upgrading AGP.
    dependencies.constraints.add(
      "androidMainImplementation",
      libs.findLibrary("androidx.concurrent.futures").get().get().toString(),
    )
  }

  @OptIn(ExperimentalWasmDsl::class)
  private fun Project.configureWasm() {
    // For development use the Gradle task 'wasmJsBrowserDevelopmentRun'.
    //
    // Release builds are built with 'wasmJsBrowserDistribution'. To test the release run
    // 'npx http-server' from the folder 'sample/app/build/dist/wasmJs/productionExecutable'.

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

  internal companion object {
    fun Project.allExportedDependencies(): Set<Any> {
      return AppPlatformPlugin.exportedDependencies()
        .plus(
          project(app.rootProjectPath)
            .subprojects
            .filter { it.subprojects.isEmpty() }
            .filter { !it.isRobotsModule() && !it.isTestingModule() && !it.isAppModule() }
        )
    }

    fun Project.configureDesktopApp() {
      composeMultiplatform.extensions.getByType(DesktopExtension::class.java).application.apply {
        mainClass = app.desktopMainFile

        nativeDistributions.targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        nativeDistributions.packageName = "software.amazon.app.platform.demo"

        // During development the major version is 0, e.g. '0.0.1'. DMG must use a
        // major version equal or greater than 1:
        //
        // Illegal version for 'Dmg': '0.0.1' is not a valid build version.
        val version = VersionNumber.parse(versionName)
        nativeDistributions.packageVersion =
          VersionNumber(max(1, version.major), version.minor, version.patch, null).toString()
      }
    }
  }

  internal enum class App(val rootProjectPath: String) {
    RECIPES(":recipes"),
    SAMPLE(":sample");

    val iosFrameworkName: String = rootProjectPath.substring(1).capitalize() + "App"
    val jsFileName: String = rootProjectPath.substring(1) + "-app.js"
    val desktopMainFile: String =
      "software.amazon.app.platform.${rootProjectPath.substring(1)}.MainKt"

    companion object {
      val Project.app: App
        get() {
          check(isAppModule())
          return when (path) {
            ":recipes:app" -> RECIPES
            ":sample:app" -> SAMPLE
            else -> throw NotImplementedError()
          }
        }
    }
  }
}
