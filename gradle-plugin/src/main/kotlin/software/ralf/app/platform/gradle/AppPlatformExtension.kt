package software.ralf.app.platform.gradle

import com.google.devtools.ksp.gradle.KspExtension
import gradle_plugin.BuildConfig.ANDROID_COMPOSE_VERSION
import gradle_plugin.BuildConfig.APP_PLATFORM_GROUP
import gradle_plugin.BuildConfig.APP_PLATFORM_VERSION
import gradle_plugin.BuildConfig.COMPOSE_MULTIPLATFORM_VERSION
import gradle_plugin.BuildConfig.KOTLIN_INJECT_ANVIL_VERSION
import gradle_plugin.BuildConfig.KOTLIN_INJECT_VERSION
import gradle_plugin.BuildConfig.MOLECULE_VERSION
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import software.ralf.app.platform.gradle.ModuleStructurePlugin.Companion.testingSourceSets

/**
 * The extension to configure the App Platform. Following options are available:
 * ```
 * appPlatform {
 *   enableKotlinInject true // false is the default
 *   enableMetro true // false is the default
 *
 *   enableMoleculePresenters true // false is the default
 *   enableMoleculePresenterBackstack true // false is the default
 *   enableModuleStructure true // false is the default
 *   enableModuleStructure {
 *     allowLibraryImplToImplDependencies true // false is the default
 *   }
 *   enableComposeUi true // false is the default
 *
 *   addPublicModuleDependencies true // false is the default
 *   addImplModuleDependencies true // false is the default
 * }
 * ```
 */
@Suppress("TooManyFunctions", "unused")
public open class AppPlatformExtension
@Inject
constructor(objects: ObjectFactory, private val project: Project) {
  private val enableKotlinInject: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds KSP and kotlin-inject as dependency. */
  public fun enableKotlinInject(enabled: Boolean) {
    if (enabled == enableKotlinInject.get()) return

    enableKotlinInject.set(enabled)
    enableKotlinInject.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableKotlinInject()
    }
  }

  internal fun isKotlinInjectEnabled(): Property<Boolean> = enableKotlinInject

  private val enableMetro: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds Metro as dependency. */
  public fun enableMetro(enabled: Boolean) {
    if (enabled == enableMetro.get()) return

    enableMetro.set(enabled)
    enableMetro.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableMetro()
    }
  }

  internal fun isMetroEnabled(): Property<Boolean> = enableMetro

  private val enableMoleculePresenters: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds the Molecule Gradle plugin as dependency and gives access to `MoleculePresenter`. */
  public fun enableMoleculePresenters(enabled: Boolean) {
    if (enabled == enableMoleculePresenters.get()) return

    enableMoleculePresenters.set(enabled)
    enableMoleculePresenters.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableMoleculePresenters()
    }
  }

  internal fun isMoleculeEnabled(): Property<Boolean> = enableMoleculePresenters

  private val enableMoleculePresenterBackstack: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /**
   * Adds the Navigation 3 presenter backstack module and enables Molecule presenters and Compose
   * UI.
   */
  public fun enableMoleculePresenterBackstack(enabled: Boolean) {
    if (enabled == enableMoleculePresenterBackstack.get()) return

    enableMoleculePresenterBackstack.set(enabled)
    enableMoleculePresenterBackstack.disallowChanges()

    if (enabled) {
      enableMoleculePresenters(true)
      enableComposeUi(true)
      project.enableMoleculePresenterBackstack()
    }
  }

  internal fun isMoleculePresenterBackstackEnabled(): Property<Boolean> =
    enableMoleculePresenterBackstack

  private val enableComposeUi: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds necessary dependencies to use Compose Multiplatform with Renderers. */
  public fun enableComposeUi(enabled: Boolean) {
    if (enabled == enableComposeUi.get()) return

    enableComposeUi.set(enabled)
    enableComposeUi.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableComposeUi()
    }
  }

  internal fun isComposeUiEnabled(): Property<Boolean> = enableComposeUi

  private val addImplModuleDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /**
   * Adds a dependency on all :impl modules. This is helpful for application modules that import all
   * implementations.
   */
  public fun addImplModuleDependencies(add: Boolean) {
    addImplModuleDependencies.set(add)
    addImplModuleDependencies.finalizeValueOnRead()

    if (add) {
      addPublicModuleDependencies(true)
    }
  }

  internal fun isAddImplModuleDependencies(): Property<Boolean> = addImplModuleDependencies

  private val addPublicModuleDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds dependencies on `:public` modules for the Presenters, Renderers and Scopes. */
  public fun addPublicModuleDependencies(add: Boolean) {
    addPublicModuleDependencies.set(add)
    addPublicModuleDependencies.finalizeValueOnRead()
  }

  internal fun isAddPublicModuleDependencies(): Property<Boolean> = addPublicModuleDependencies

  private val enableModuleStructure: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)
  private val moduleStructureOptions: ModuleStructureOptions =
    objects.newInstance(ModuleStructureOptions::class.java)

  /** Sets up this module to use our recommended module structure and applies certain defaults. */
  public fun enableModuleStructure(enable: Boolean) {
    if (enable == enableModuleStructure.get()) return

    enableModuleStructure.set(enable)
    enableModuleStructure.disallowChanges()

    if (enable) {
      project.plugins.apply(ModuleStructurePlugin::class.java)
    }
  }

  /** Sets up and configures this module to use our recommended module structure. */
  public fun enableModuleStructure(action: Action<ModuleStructureOptions>) {
    action.execute(moduleStructureOptions)
    enableModuleStructure(true)
  }

  internal fun isModuleStructureEnabled(): Property<Boolean> = enableModuleStructure

  internal fun moduleStructureOptions(): ModuleStructureOptions = moduleStructureOptions

  internal companion object {
    internal val Project.appPlatform: AppPlatformExtension
      get() = extensions.getByType(AppPlatformExtension::class.java)
  }
}

@Suppress("LongMethod")
private fun Project.enableKotlinInject() {
  plugins.apply(PluginIds.KSP)

  val kspExtension = extensions.getByType(KspExtension::class.java)

  // Disable this processor, because we implement our own version in order to support the
  // Scoped interface.
  kspExtension.arg(
    "software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor",
    "disabled",
  )

  fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
    add(kspConfigurationName, "me.tatarka.inject:kotlin-inject-compiler-ksp:$KOTLIN_INJECT_VERSION")
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION",
    )
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-impl-code-generators:$APP_PLATFORM_VERSION",
    )
    add(
      kspConfigurationName,
      "software.amazon.lastmile.kotlin.inject.anvil:compiler:$KOTLIN_INJECT_ANVIL_VERSION",
    )
  }

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("me.tatarka.inject:kotlin-inject-runtime:$KOTLIN_INJECT_VERSION")
      implementation("$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:kotlin-inject-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION")
      implementation(
        "software.amazon.lastmile.kotlin.inject.anvil:runtime:$KOTLIN_INJECT_ANVIL_VERSION"
      )
      implementation(
        "software.amazon.lastmile.kotlin.inject.anvil:runtime-optional:" +
          KOTLIN_INJECT_ANVIL_VERSION
      )
    }

    kmpExtension.targets.configureEach { target ->
      addKspDependenciesWhenConfigExists(target) { configName ->
        dependencies.addKspProcessorDependencies(configName)
      }
    }
  }

  withJvmOrAndroidPlugin {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:kotlin-inject-public:$APP_PLATFORM_VERSION",
    )
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION",
    )
    dependencies.add(
      "implementation",
      "software.amazon.lastmile.kotlin.inject.anvil:runtime:$KOTLIN_INJECT_ANVIL_VERSION",
    )
    dependencies.add(
      "implementation",
      "software.amazon.lastmile.kotlin.inject.anvil:runtime-optional:$KOTLIN_INJECT_ANVIL_VERSION",
    )
    dependencies.add(
      "implementation",
      "me.tatarka.inject:kotlin-inject-runtime:$KOTLIN_INJECT_VERSION",
    )
    dependencies.addKspProcessorDependencies("ksp")
  }
}

private fun Project.enableMetro() {
  plugins.apply(PluginIds.METRO)

  val useMetroKsp =
    providers.gradleProperty("app.platform.metro.ksp").map(String::toBoolean).orElse(false).get()

  if (useMetroKsp) {
    enableMetroKsp()
  } else {
    enableMetroCompilerPlugin()
  }
}

private fun Project.enableMetroKsp() {
  plugins.apply(PluginIds.KSP)

  fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:metro-contribute-impl-code-generators:$APP_PLATFORM_VERSION",
    )
  }

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")
    }

    kmpExtension.targets.configureEach { target ->
      addKspDependenciesWhenConfigExists(target) { configName ->
        dependencies.addKspProcessorDependencies(configName)
      }
    }
  }

  withJvmOrAndroidPlugin {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")
    dependencies.addKspProcessorDependencies("ksp")
  }
}

private fun Project.enableMetroCompilerPlugin() {
  val compilerPluginDependency =
    "$APP_PLATFORM_GROUP:metro-contribute-impl-compiler-plugin:$APP_PLATFORM_VERSION"

  fun DependencyHandler.addCompilerPluginDependencies() {
    add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
    add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, compilerPluginDependency)
  }

  fun addAgpBuiltInKotlinCompilerPluginDependencies() {
    configurations.configureEach { configuration ->
      if (
        configuration.name.startsWith(PLUGIN_CLASSPATH_CONFIGURATION_NAME) &&
          configuration.name != PLUGIN_CLASSPATH_CONFIGURATION_NAME
      ) {
        dependencies.add(configuration.name, compilerPluginDependency)
      }
    }
  }

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")
    }
    dependencies.addCompilerPluginDependencies()
  }

  withLegacyKotlinJvmOrAndroidPlugin {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")

    dependencies.add("implementation", "$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")

    dependencies.addCompilerPluginDependencies()
  }

  withAgpBuiltInKotlinAndroidPlugin {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")
    addAgpBuiltInKotlinCompilerPluginDependencies()
  }
}

private fun Project.enableMoleculePresenters() {
  plugins.apply(PluginIds.COMPOSE_COMPILER)

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("app.cash.molecule:molecule-runtime:$MOLECULE_VERSION")
      implementation("$APP_PLATFORM_GROUP:presenter-molecule-public:$APP_PLATFORM_VERSION")
    }
    testingSourceSets.forEach { sourceSetName ->
      kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
        implementation("$APP_PLATFORM_GROUP:presenter-molecule-testing:$APP_PLATFORM_VERSION")
      }
    }
  }

  withJvmOrAndroidPlugin {
    dependencies.add("implementation", "app.cash.molecule:molecule-runtime:$MOLECULE_VERSION")
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:presenter-molecule-public:$APP_PLATFORM_VERSION",
    )
    testingSourceSets.forEach { sourceSetName ->
      dependencies.add(
        sourceSetName,
        "$APP_PLATFORM_GROUP:presenter-molecule-testing:$APP_PLATFORM_VERSION",
      )
    }
  }
}

private fun Project.enableMoleculePresenterBackstack() {
  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("$APP_PLATFORM_GROUP:presenter-backstack-nav3-public:$APP_PLATFORM_VERSION")
    }
    testingSourceSets.forEach { sourceSetName ->
      kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
        implementation("$APP_PLATFORM_GROUP:presenter-backstack-nav3-testing:$APP_PLATFORM_VERSION")
      }
    }
  }

  withJvmOrAndroidPlugin {
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:presenter-backstack-nav3-public:$APP_PLATFORM_VERSION",
    )
    testingSourceSets.forEach { sourceSetName ->
      dependencies.add(
        sourceSetName,
        "$APP_PLATFORM_GROUP:presenter-backstack-nav3-testing:$APP_PLATFORM_VERSION",
      )
    }
  }
}

private fun Project.enableComposeUi() {
  val robotComposeDependency =
    "$APP_PLATFORM_GROUP:robot-compose-multiplatform-public:$APP_PLATFORM_VERSION"

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    plugins.apply(PluginIds.COMPOSE_COMPILER)
    plugins.apply(PluginIds.COMPOSE_MULTIPLATFORM)

    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("org.jetbrains.compose.foundation:foundation:$COMPOSE_MULTIPLATFORM_VERSION")
      implementation("org.jetbrains.compose.runtime:runtime:$COMPOSE_MULTIPLATFORM_VERSION")

      implementation(
        "$APP_PLATFORM_GROUP:renderer-compose-multiplatform-public:$APP_PLATFORM_VERSION"
      )

      if (isRobotsModule()) {
        implementation(robotComposeDependency)
      }
    }

    if (isAppModule()) {
      testingSourceSets.forEach { sourceSetName ->
        kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
          implementation(robotComposeDependency)
        }
      }
    }
  }

  plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
    plugins.apply(PluginIds.COMPOSE_COMPILER)

    android.buildFeatures.compose = true

    dependencies.add("implementation", "androidx.compose.runtime:runtime:$ANDROID_COMPOSE_VERSION")
    dependencies.add(
      "implementation",
      "androidx.compose.foundation:foundation:$ANDROID_COMPOSE_VERSION",
    )
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:renderer-compose-multiplatform-public:$APP_PLATFORM_VERSION",
    )

    if (isRobotsModule()) {
      dependencies.add("implementation", robotComposeDependency)
    }

    if (isAppModule()) {
      dependencies.add("androidTestImplementation", robotComposeDependency)
    }
  }
}

private fun Project.addKspDependenciesWhenConfigExists(
  target: KotlinTarget,
  block: (String) -> Unit,
) {
  if (target.name != "metadata") {
    target.compilations.configureEach { compilation ->
      fun configExists(name: String): Boolean = configurations.any { it.name == name }

      // Derive the KSP configuration name from the target name and compilation name.
      // For main compilations: ksp<TargetName> (e.g. kspDesktop, kspIosSimulatorArm64)
      // For test compilations: ksp<TargetName>Test (e.g. kspDesktopTest)
      val targetName = target.name.capitalize()
      var configName =
        if (compilation.name == "main") {
          "ksp$targetName"
        } else {
          "ksp$targetName${compilation.name.capitalize()}"
        }

      if (!configExists(configName) && target.platformType == KotlinPlatformType.androidJvm) {
        // Android has different naming for some reason.
        //
        // E.g. for instrumentation tests 'kspAndroidDebugAndroidTest' should actually be
        // 'kspAndroidAndroidTestDebug', but we will use 'kspAndroidAndroidTest'.
        //
        // For unit tests 'kspAndroidDebugUnitTest' should actually be 'kspAndroidTestDebug',
        // but we will use 'kspAndroidTest'.
        when {
          configName.endsWith("AndroidTest") -> configName = "kspAndroidAndroidTest"
          configName.endsWith("UnitTest") -> configName = "kspAndroidTest"
        }
      }

      // Check again if the config exists.
      if (configExists(configName)) {
        block(configName)
      }
    }
  }
}
