package software.ralf.app.platform.gradle.buildsrc

import com.google.devtools.ksp.gradle.KspExtension
import guru.nidi.graphviz.engine.Format
import io.github.terrakok.KmpHierarchyConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformBuildSrc
import software.ralf.app.platform.gradle.buildsrc.Platform.Companion.allPlatforms

public open class KmpPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.KOTLIN_MULTIPLATFORM)

    target.configureCommonKotlin()
    target.configureCoroutines()
    target.configureTests()
    target.configureDetekt()

    target.addExtraSourceSets()
    target.configureHierarchyPlugin()
  }

  private fun Project.configureCommonKotlin() {
    // KGP derives compilation module names, including metadata KLIB unique names, from the
    // archive base name. Use the normalized project path to distinguish modules named "public"
    // or "impl".
    extensions.getByType(BasePluginExtension::class.java).archivesName.set(safePathString)

    kmpExtension.applyDefaultHierarchyTemplate()

    dependencies.add(
      "commonMainApi",
      dependencies.platform(libs.findLibrary("kotlin.bom").get().get().toString()),
    )

    // Only for tests.
    kmpExtension.sourceSets
      .getByName("commonTest")
      .languageSettings
      .optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")

    kmpExtension.compilerOptions {
      // Unfortunately, we cannot set this to true. It produces warnings for generated code,
      // which cannot be excluded.
      extraWarnings.set(false)

      allWarningsAsErrors.set(appPlatformBuildSrc.isKotlinWarningsAsErrors())
    }

    kmpExtension.targets.configureEach { target ->
      target.compilations.configureEach { compilation ->
        compilation.compileTaskProvider.configure { task ->
          with(task.compilerOptions) {
            if ("test" in task.name.lowercase() || path == ":internal:testing") {
              freeCompilerArgs.add("-Xexpect-actual-classes")
              freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
          }
        }
      }
    }

    allPlatforms().forEach { platform -> platform.configurePlatform() }
  }

  private fun Project.configureCoroutines() {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation(libs.findLibrary("coroutines.core").get().get().toString())
    }

    testingSourceSets.forEach { sourceSetName ->
      kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
        // Use api for main source sets (testing utility modules) so downstream modules
        // get transitive access. Use implementation for test source sets since api is
        // deprecated there in Kotlin 2.3.
        val isTestSourceSet = sourceSetName.contains("Test", ignoreCase = true)
        if (isTestSourceSet) {
          implementation(libs.findLibrary("coroutines.test").get().get().toString())
          implementation(libs.findLibrary("turbine").get().get().toString())
        } else {
          api(libs.findLibrary("coroutines.test").get().get().toString())
          api(libs.findLibrary("turbine").get().get().toString())
        }
      }
    }

    allPlatforms().forEach { platform -> platform.configureCoroutines() }
  }

  private fun Project.configureTests() {
    testingSourceSets.forEach { sourceSetName ->
      kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
        val isTestSourceSet = sourceSetName.contains("Test", ignoreCase = true)
        if (isTestSourceSet) {
          implementation(kotlin("test"))
          implementation(libs.findLibrary("assertk").get().get().toString())
        } else {
          api(kotlin("test"))
          api(libs.findLibrary("assertk").get().get().toString())
        }
      }
    }

    releaseTask.configure { task ->
      task.dependsOn(allPlatforms().mapNotNull { it.unitTestTaskName })
    }
  }

  private fun Project.addExtraSourceSets() {
    val platforms = allPlatforms()
    if (platforms.any { it is Platform.Ios } && platforms.any { it is Platform.DesktopPlatform }) {
      setOf("Main", "Test").forEach { suffix ->
        val common = kmpExtension.sourceSets.getByName("common$suffix")

        val appleAndDesktop = kmpExtension.sourceSets.create("appleAndDesktop$suffix")
        appleAndDesktop.dependsOn(common)

        kmpExtension.sourceSets.named("apple$suffix").configure { it.dependsOn(appleAndDesktop) }
        kmpExtension.sourceSets.named("desktop$suffix").configure { it.dependsOn(appleAndDesktop) }

        val noWasmJs = kmpExtension.sourceSets.create("noWasmJs$suffix")
        noWasmJs.dependsOn(common)

        appleAndDesktop.dependsOn(noWasmJs)
        kmpExtension.sourceSets.named("native$suffix").configure { it.dependsOn(noWasmJs) }
        if (suffix == "Main") {
          kmpExtension.sourceSets.named("android$suffix").configure { it.dependsOn(noWasmJs) }
        } else {
          kmpExtension.sourceSets.named("androidHostTest").configure { it.dependsOn(noWasmJs) }
        }
      }
    }
  }

  private fun Project.configureHierarchyPlugin() {
    plugins.apply(Plugins.KOTLIN_HIERARCHY)

    (extensions.getByType(KotlinMultiplatformExtension::class.java) as ExtensionAware)
      .extensions
      .getByType(KmpHierarchyConfig::class.java)
      .run {
        formats(Format.PNG, Format.SVG)
        withTestHierarchy = true
      }
  }

  internal companion object {
    val Project.kmpExtension: KotlinMultiplatformExtension
      get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

    val Project.composeMultiplatform: ComposeExtension
      get() = extensions.getByType(ComposeExtension::class.java)

    fun Project.enableCompose() {
      plugins.apply(Plugins.COMPOSE_COMPILER)
      plugins.apply(Plugins.COMPOSE_MULTIPLATFORM)

      val composeVersion = libs.findVersion("compose.multiplatform").get().requiredVersion

      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
        implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
      }

      allPlatforms().forEach { platform -> platform.configureCompose() }
    }

    fun Project.enableKotlinInject() {
      enableKsp()

      val kspExtension = extensions.getByType(KspExtension::class.java)

      // Disable this processor, because we implement our own version in order to support the
      // Scoped interface.
      kspExtension.arg(
        "software.amazon.lastmile.kotlin.inject.anvil.processor." + "ContributesBindingProcessor",
        "disabled",
      )

      if (isKmpModule) {
        kmpExtension.sourceSets.getByName("commonMain").dependencies {
          implementation(libs.findLibrary("kotlin.inject.runtime").get().get().toString())
          implementation(libs.findLibrary("kotlin.inject.anvil.runtime").get().get().toString())
          implementation(
            libs.findLibrary("kotlin.inject.anvil.runtime.optional").get().get().toString()
          )

          if (path != ":di-common:public" && path != ":kotlin-inject:public") {
            implementation(project(":di-common:public"))
            implementation(project(":kotlin-inject:public"))
            if (!path.startsWith(":kotlin-inject-extensions:contribute:")) {
              implementation(project(":kotlin-inject-extensions:contribute:public"))
            }
          }
        }
      } else {
        dependencies.add(
          "implementation",
          libs.findLibrary("kotlin.inject.runtime").get().get().toString(),
        )
        dependencies.add(
          "implementation",
          libs.findLibrary("kotlin.inject.anvil.runtime").get().get().toString(),
        )
        dependencies.add(
          "implementation",
          libs.findLibrary("kotlin.inject.anvil.runtime.optional").get().get().toString(),
        )
        if (path != ":di-common:public" && path != ":kotlin-inject:public") {
          dependencies.add("implementation", project(":di-common:public"))
          dependencies.add("implementation", project(":kotlin-inject:public"))
          if (!path.startsWith(":kotlin-inject-extensions:contribute:")) {
            dependencies.add(
              "implementation",
              project(":kotlin-inject-extensions:contribute:public"),
            )
          }
        }
      }

      fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
        add(kspConfigurationName, libs.findLibrary("kotlin.inject.ksp").get().get().toString())
        add(
          kspConfigurationName,
          libs.findLibrary("kotlin.inject.anvil.compiler").get().get().toString(),
        )

        // Avoid creating a circular dependency.
        if (
          path != ":di-common:public" &&
            path != ":kotlin-inject:public" &&
            !path.startsWith(":kotlin-inject-extensions:contribute:")
        ) {
          add(kspConfigurationName, project(":kotlin-inject-extensions:contribute:public"))
          add(
            kspConfigurationName,
            project(":kotlin-inject-extensions:contribute:impl-code-generators"),
          )
        }
      }

      if (isKmpModule) {
        kmpExtension.targets.configureEach { target ->
          addKspDependenciesWhenConfigExists(target) { configurationName ->
            dependencies.addKspProcessorDependencies(configurationName)
          }
        }
      } else {
        dependencies.addKspProcessorDependencies("ksp")
      }
    }

    fun Project.enableMetro() {
      plugins.apply(Plugins.METRO)

      val useMetroKsp =
        providers
          .gradleProperty("app.platform.metro.ksp")
          .map(String::toBoolean)
          .orElse(false)
          .get()

      if (useMetroKsp) {
        enableMetroKsp()
      } else {
        enableMetroCompilerPlugin()
      }
    }

    private fun Project.enableMetroKsp() {
      enableKsp()

      if (isKmpModule) {
        kmpExtension.sourceSets.getByName("commonMain").dependencies {
          implementation(project(":di-common:public"))
          implementation(project(":metro:public"))
        }
      } else {
        dependencies.add("implementation", project(":metro:public"))
      }

      fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
        add(kspConfigurationName, project(":metro-extensions:contribute:impl-code-generators"))
      }

      if (isKmpModule) {
        kmpExtension.targets.configureEach { target ->
          addKspDependenciesWhenConfigExists(target) { configurationName ->
            dependencies.addKspProcessorDependencies(configurationName)
          }
        }
      } else {
        dependencies.addKspProcessorDependencies("ksp")
      }
    }

    private fun Project.enableMetroCompilerPlugin() {
      if (isKmpModule) {
        kmpExtension.sourceSets.getByName("commonMain").dependencies {
          implementation(project(":di-common:public"))
          implementation(project(":metro:public"))
        }
      } else {
        dependencies.add("implementation", project(":metro:public"))
      }

      fun DependencyHandler.addCompilerPluginDependencies() {
        add(
          PLUGIN_CLASSPATH_CONFIGURATION_NAME,
          project(":metro-extensions:contribute:impl-compiler-plugin"),
        )
        add(
          NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME,
          project(":metro-extensions:contribute:impl-compiler-plugin"),
        )
      }

      plugins.withId(Plugins.KOTLIN_MULTIPLATFORM) { dependencies.addCompilerPluginDependencies() }
      plugins.withId(Plugins.KOTLIN_JVM) { dependencies.addCompilerPluginDependencies() }
    }

    private fun Project.enableKsp() {
      plugins.apply(Plugins.KSP)
    }

    private fun Project.addKspDependenciesWhenConfigExists(
      target: KotlinTarget,
      block: (String) -> Unit,
    ) {
      if (target.name == "metadata") return

      target.compilations.configureEach { compilation ->
        fun configExists(name: String): Boolean = configurations.any { it.name == name }

        val targetName = target.name.capitalize()
        var configurationName =
          if (compilation.name == "main") {
            "ksp$targetName"
          } else {
            "ksp$targetName${compilation.name.capitalize()}"
          }

        if (
          !configExists(configurationName) && target.platformType == KotlinPlatformType.androidJvm
        ) {
          configurationName =
            when {
              configurationName.endsWith("AndroidTest") -> "kspAndroidAndroidTest"
              configurationName.endsWith("UnitTest") -> "kspAndroidTest"
              else -> configurationName
            }
        }

        if (configExists(configurationName)) {
          block(configurationName)
        }
      }
    }

    fun Project.enableMolecule() {
      plugins.apply(Plugins.COMPOSE_COMPILER)
      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementation(libs.findLibrary("molecule.runtime").get().get().toString())
        implementation(libs.findLibrary("compose.multiplatform.runtime").get().get().toString())
        implementation(libs.findLibrary("compose.runtime.retain").get().get().toString())
      }
    }

    private val Project.testingSourceSets
      get() = buildList {
        add("commonTest")
        if (useTestDependenciesInMain()) {
          add("commonMain")
        }
      }
  }
}
