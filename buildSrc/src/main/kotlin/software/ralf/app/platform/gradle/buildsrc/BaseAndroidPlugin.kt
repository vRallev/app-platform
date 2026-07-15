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

    android.lint {
      warningsAsErrors = true
      htmlReport = true
      disable +=
        setOf(
          "GradleDependency",
          "ObsoleteLintCustomCheck",
          "NewerVersionAvailable",
          "AndroidGradlePluginVersion",
          "OldTargetApi",
        )
    }

    releaseTask.configure { it.dependsOn("lintDebug") }
  }

  internal companion object {
    internal fun Project.enableInstrumentedTests() {
      releaseTask.configure {
        it.dependsOn("assembleDebugAndroidTest")
        it.dependsOn("emulatorCheck")
      }

      android.defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments += "clearPackageData" to "true"
      }

      android.testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"

      dependencies.add(
        "androidTestUtil",
        libs.findLibrary("androidx.test.orchestrator").get().get().toString(),
      )
      dependencies.add(
        "androidTestImplementation",
        libs.findLibrary("androidx.test.runner").get().get().toString(),
      )
      dependencies.add(
        "androidTestImplementation",
        libs.findLibrary("androidx.test.rules").get().get().toString(),
      )
      dependencies.add(
        "androidTestImplementation",
        libs.findLibrary("androidx.test.junit").get().get().toString(),
      )
      dependencies.add(
        "androidTestImplementation",
        libs.findLibrary("kotlin.test").get().get().toString(),
      )
      dependencies.add(
        "androidTestImplementation",
        libs.findLibrary("assertk").get().get().toString(),
      )

      @Suppress("UnstableApiUsage")
      android.testOptions.managedDevices.localDevices.create("emulator") {
        // Use device profiles you typically see in Android Studio.
        it.device = "Pixel 3"
        it.apiLevel = 30
        it.require64Bit = true
        it.systemImageSource = "aosp-atd"
      }
    }
  }
}
