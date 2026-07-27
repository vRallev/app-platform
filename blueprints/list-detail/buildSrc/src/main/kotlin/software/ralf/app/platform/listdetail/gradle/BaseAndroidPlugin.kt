package software.ralf.app.platform.listdetail.gradle

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class BaseAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.configureAndroid()
  }

  private fun Project.configureAndroid() {
    val android = extensions.getByType(ApplicationExtension::class.java)
    android.compileSdk = androidCompileSdk
    android.defaultConfig {
      applicationId = "software.ralf.app.platform.listdetail"
      minSdk = androidMinSdk
      targetSdk = androidTargetSdk
      versionCode = 1
      versionName = this@configureAndroid.versionName
    }

    android.compileOptions {
      sourceCompatibility = javaVersion
      targetCompatibility = javaVersion
    }
    android.testOptions.unitTests {
      isIncludeAndroidResources = false
      isReturnDefaultValues = true
    }
    configureAndroidLint(android.lint)
    configureAndroidPackaging(android.packaging)
    configureAndroidInstrumentedTests(android)

    releaseTask.configure { it.dependsOn("lintDebug") }
  }

  private fun Project.configureAndroidInstrumentedTests(android: ApplicationExtension) {
    android.defaultConfig {
      testInstrumentationRunner = "software.ralf.app.platform.listdetail.TestRunner"
      testInstrumentationRunnerArguments += "clearPackageData" to "true"
    }
    android.testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"
    @Suppress("UnstableApiUsage")
    android.testOptions.managedDevices.localDevices.create("emulator") {
      it.device = "Pixel 3"
      it.apiLevel = 30
      it.require64Bit = true
      it.systemImageSource = "aosp-atd"
    }
    dependencies.add(
      "androidTestUtil",
      libs.findLibrary("androidx-test-orchestrator").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("androidx-test-junit").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("androidx-test-rules").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("androidx-test-runner").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("compose-ui-test-junit4").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("compose-ui-test-junit4-android").get().get().toString(),
    )
    dependencies.add(
      "androidTestImplementation",
      libs.findLibrary("compose-ui-test-manifest").get().get().toString(),
    )

    releaseTask.configure { it.dependsOn("assembleDebugAndroidTest") }
  }
}
