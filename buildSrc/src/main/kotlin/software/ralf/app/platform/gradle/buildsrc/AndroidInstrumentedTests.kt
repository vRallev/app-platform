package software.ralf.app.platform.gradle.buildsrc

import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.Project

internal const val ANDROID_TEST_INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
internal const val ANDROID_TEST_EXECUTION = "ANDROIDX_TEST_ORCHESTRATOR"
internal val ANDROID_TEST_INSTRUMENTATION_RUNNER_ARGUMENTS = mapOf("clearPackageData" to "true")

internal fun Project.configureAppPlatformInstrumentedTests(
  implementationConfigurationName: String
) {
  releaseTask.configure { it.dependsOn("emulatorCheck") }

  dependencies.add(
    "androidTestUtil",
    libs.findLibrary("androidx.test.orchestrator").get().get().toString(),
  )
  listOf(
      "androidx.test.runner",
      "androidx.test.rules",
      "androidx.test.junit",
      "kotlin.test",
      "assertk",
    )
    .forEach { libraryName ->
      dependencies.add(
        implementationConfigurationName,
        libs.findLibrary(libraryName).get().get().toString(),
      )
    }
}

@Suppress("UnstableApiUsage")
internal fun ManagedVirtualDevice.configureAppPlatformEmulator() {
  // Use device profiles you typically see in Android Studio.
  device = "Pixel 3"
  apiLevel = 30
  require64Bit = true
  systemImageSource = "aosp-atd"
}
