package software.ralf.app.platform.gradle.buildsrc

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public open class BaseAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.configureAndroid()
  }

  private fun Project.configureAndroid() {
    val android = android

    android.compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
    android.defaultConfig.minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()

    when (android) {
      is LibraryExtension -> {
        android.lint.targetSdk = libs.findVersion("android.targetSdk").get().requiredVersion.toInt()
        android.testOptions.targetSdk =
          libs.findVersion("android.targetSdk").get().requiredVersion.toInt()
        android.defaultConfig.multiDexEnabled = true
      }

      is ApplicationExtension -> {
        android.defaultConfig {
          targetSdk = libs.findVersion("android.targetSdk").get().requiredVersion.toInt()
          multiDexEnabled = true

          applicationId = "software.ralf.app.platform.demo"
          versionCode = 1
          versionName = this@configureAndroid.versionName
        }
      }
    }

    android.packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    android.buildTypes.getByName("release").isMinifyEnabled = false

    android.compileOptions.sourceCompatibility = javaVersion
    android.compileOptions.targetCompatibility = javaVersion

    android.testOptions.unitTests {
      // Disable including Android resources in tests. None of our modules need them and it avoids
      // running into issues with Gradle 9: https://issuetracker.google.com/issues/411739086
      isIncludeAndroidResources = false

      isReturnDefaultValues = true
    }

    android.lint.configureAppPlatformLint()

    releaseTask.configure { it.dependsOn("lintDebug") }
  }

  internal companion object {
    internal fun Project.enableAndroidInstrumentedTests() {
      releaseTask.configure { it.dependsOn("assembleDebugAndroidTest") }
      configureAppPlatformInstrumentedTests("androidTestImplementation")

      android.defaultConfig.apply {
        testInstrumentationRunner = ANDROID_TEST_INSTRUMENTATION_RUNNER
        testInstrumentationRunnerArguments += ANDROID_TEST_INSTRUMENTATION_RUNNER_ARGUMENTS
      }

      android.testOptions.execution = ANDROID_TEST_EXECUTION

      @Suppress("UnstableApiUsage")
      android.testOptions.managedDevices.localDevices.create("emulator") {
        it.configureAppPlatformEmulator()
      }
    }
  }
}
