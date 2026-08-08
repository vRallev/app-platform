package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class KmpAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.KOTLIN_MULTIPLATFORM)
    target.plugins.apply(Plugins.ANDROID_LINT)
    target.plugins.apply(Plugins.ANDROID_KMP_LIBRARY)

    target.configureAndroid()
  }

  private fun Project.configureAndroid() {
    val targetSdk = libs.findVersion("android.targetSdk").get().requiredVersion.toInt()

    androidKmpTarget.apply {
      compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
      minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()
      compilerOptions { jvmTarget.set(javaTarget) }

      packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

      withHostTest {
        isIncludeAndroidResources = false
        isReturnDefaultValues = true
      }

      lint {
        this.targetSdk = targetSdk
        configureAppPlatformLint()
      }
    }

    releaseTask.configure { it.dependsOn("lintAndroidMain") }
  }

  internal companion object {
    internal fun Project.enableAndroidResources(enabled: Boolean) {
      androidKmpTarget.androidResources.enable = enabled
    }

    internal fun Project.enableKmpInstrumentedTests() {
      androidKmpTarget.withDeviceTest {
        instrumentationRunner = ANDROID_TEST_INSTRUMENTATION_RUNNER
        instrumentationRunnerArguments += ANDROID_TEST_INSTRUMENTATION_RUNNER_ARGUMENTS
        execution = ANDROID_TEST_EXECUTION

        managedDevices.localDevices.create("emulator") {
          it.configureAppPlatformEmulator()
        }
      }

      configureAppPlatformInstrumentedTests("androidDeviceTestImplementation")
    }
  }
}
