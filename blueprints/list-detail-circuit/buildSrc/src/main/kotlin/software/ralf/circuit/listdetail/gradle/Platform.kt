package software.ralf.circuit.listdetail.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

internal sealed interface Platform {
  fun configurePlatform()

  fun configureCompose() = Unit

  private class Android(private val project: Project) : Platform {
    override fun configurePlatform() {
      project.plugins.apply(Plugins.ANDROID_MULTIPLATFORM)

      with(project.androidKmpTarget) {
        compileSdk = project.androidCompileSdk
        minSdk = project.androidMinSdk
        compilerOptions { jvmTarget.set(project.jvmTarget) }
        withHostTest { isReturnDefaultValues = true }
        lint {
          configureAndroidLint(this)
          targetSdk = project.androidTargetSdk
        }
        configureAndroidPackaging(packaging)
      }

      project.releaseTask.configure { it.dependsOn("lintAnalyzeAndroidHostTest") }
    }

    override fun configureCompose() {
      project.androidKmpTarget.androidResources.enable = true
    }
  }

  private class Desktop(private val project: Project) : Platform {
    override fun configurePlatform() {
      project.kmpExtension.jvm("desktop").compilerOptions { jvmTarget.set(project.jvmTarget) }

      with(project.extensions.getByType(JavaPluginExtension::class.java)) {
        sourceCompatibility = project.javaVersion
        targetCompatibility = project.javaVersion
      }
    }

    override fun configureCompose() {
      val composeVersion = project.libs.findVersion("compose-multiplatform").get().requiredVersion
      val desktopArtifact =
        "org.jetbrains.compose.desktop:desktop-jvm-${currentOsTarget()}:$composeVersion"

      project.kmpExtension.sourceSets.getByName("desktopMain").dependencies {
        implementation(project.libs.findLibrary("coroutines-swing").get().get().toString())
        implementation(desktopArtifact)
      }
      project.kmpExtension.sourceSets.getByName("desktopTest").dependencies {
        implementation("org.jetbrains.compose.ui:ui-test-junit4:$composeVersion")
        implementation(desktopArtifact)
      }
    }

    private fun currentOsTarget(): String {
      val os = System.getProperty("os.name").lowercase()
      val arch = System.getProperty("os.arch").lowercase()
      return when {
        os.contains("mac") || os.contains("darwin") ->
          if (arch.contains("aarch64") || arch.contains("arm")) "macos-arm64" else "macos-x64"
        os.contains("win") ->
          if (arch.contains("aarch64") || arch.contains("arm")) {
            "windows-arm64"
          } else {
            "windows-x64"
          }
        else -> if (arch.contains("aarch64") || arch.contains("arm")) "linux-arm64" else "linux-x64"
      }
    }
  }

  private abstract class Ios : Platform {
    protected abstract val project: Project
    protected abstract val target: KotlinNativeTarget

    override fun configurePlatform() {
      val configuredTarget = target
      if (project.path != ":app-framework:impl") return

      configuredTarget.binaries.framework {
        baseName = "ListDetailCircuitApp"
        isStatic = true
        binaryOption("bundleId", "software.ralf.circuit.listdetail")
      }
    }
  }

  private class IosSimulatorArm64(override val project: Project) : Ios() {
    override val target: KotlinNativeTarget by lazy { project.kmpExtension.iosSimulatorArm64() }
  }

  private class IosArm64(override val project: Project) : Ios() {
    override val target: KotlinNativeTarget by lazy { project.kmpExtension.iosArm64() }
  }

  private class Wasm(private val project: Project) : Platform {
    @OptIn(ExperimentalWasmDsl::class)
    override fun configurePlatform() {
      project.kmpExtension.wasmJs {
        browser {
          outputModuleName.set(project.safePathString)

          if (project.path == ":app:web") {
            commonWebpackConfig {
              it.outputFileName = "list-detail.js"
              it.devServer = it.devServer ?: KotlinWebpackConfig.DevServer()
            }
          }
        }

        if (project.path == ":app:web") {
          binaries.executable()
        }
      }
    }
  }

  companion object {
    fun Project.platforms(): Set<Platform> = buildSet {
      when (path) {
        ":app:android" -> add(Android(project = this@platforms))
        ":app:desktop" -> add(Desktop(project = this@platforms))
        ":app:web" -> add(Wasm(project = this@platforms))
        else -> {
          add(Android(project = this@platforms))
          add(Desktop(project = this@platforms))
          add(IosSimulatorArm64(project = this@platforms))
          add(IosArm64(project = this@platforms))
          add(Wasm(project = this@platforms))
        }
      }
    }
  }
}
