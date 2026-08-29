package software.ralf.circuit.listdetail.gradle

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import software.ralf.app.platform.gradle.ModuleStructurePlugin.Companion.namespace
import software.ralf.app.platform.gradle.moduleType
import software.ralf.circuit.listdetail.gradle.Platform.Companion.platforms

internal class KmpPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.KOTLIN_MULTIPLATFORM)

    target.configureKotlin()
    target.configureDependencies()
    target.configureComposeDependencies()
    target.configureMetro()
    target.configureDetekt()
  }

  private fun Project.configureKotlin() {
    extensions.getByType(BasePluginExtension::class.java).archivesName.set(safePathString)
    kmpExtension.applyDefaultHierarchyTemplate()
    kmpExtension.compilerOptions {
      extraWarnings.set(false)
      allWarningsAsErrors.set(ci)
    }

    platforms().forEach { it.configurePlatform() }
  }

  private fun Project.configureDependencies() {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation(libs.findLibrary("coroutines-core").get().get().toString())
    }

    val testDependenciesInMain = moduleType.useTestDependenciesInMain
    val testSourceSetName = if (testDependenciesInMain) "commonMain" else "commonTest"
    kmpExtension.sourceSets.getByName(testSourceSetName).dependencies {
      if (testDependenciesInMain) {
        api(kotlin("test"))
        api(libs.findLibrary("assertk").get().get().toString())
        api(libs.findLibrary("coroutines-test").get().get().toString())
      } else {
        implementation(kotlin("test"))
        implementation(libs.findLibrary("assertk").get().get().toString())
        implementation(libs.findLibrary("coroutines-test").get().get().toString())
      }
    }
  }

  private fun Project.configureComposeDependencies() {
    plugins.withId(Plugins.COMPOSE_MULTIPLATFORM) {
      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementation(libs.findLibrary("compose-runtime").get().get().toString())
        implementation(libs.findLibrary("compose-ui").get().get().toString())
        implementation(libs.findLibrary("compose-foundation").get().get().toString())
        implementation(libs.findLibrary("compose-resources").get().get().toString())
      }

      val composeExtension = extensions.getByType(ComposeExtension::class.java)
      (composeExtension as ExtensionAware)
        .extensions
        .getByType(ResourcesExtension::class.java)
        .packageOfResClass = "${namespace()}.generated.resources"

      platforms().forEach { it.configureCompose() }
    }
  }

  private fun Project.configureMetro() {
    plugins.withId(Plugins.METRO) {
      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementation(libs.findLibrary("metro-runtime").get().get().toString())
      }
      extensions
        .getByType(MetroPluginExtension::class.java)
        .unusedGraphInputsSeverity
        .set(DiagnosticSeverity.NONE)
    }
  }
}
