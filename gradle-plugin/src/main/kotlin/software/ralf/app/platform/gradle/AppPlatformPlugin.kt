package software.ralf.app.platform.gradle

import gradle_plugin.BuildConfig.APP_PLATFORM_GROUP
import gradle_plugin.BuildConfig.APP_PLATFORM_VERSION
import org.gradle.api.Plugin
import org.gradle.api.Project
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform
import software.ralf.app.platform.gradle.ModuleStructurePlugin.Companion.testingSourceSets

/** The Gradle plugin to make the integration of the App Platform easy. */
@Suppress("unused")
public open class AppPlatformPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create("appPlatform", AppPlatformExtension::class.java)

    target.afterEvaluate {
      target.addPublicDependencies()
      target.addImplDependencies()
    }
  }

  @Suppress("LongMethod")
  private fun Project.addPublicDependencies() {
    if (!appPlatform.isAddPublicModuleDependencies().get()) {
      // If disabled, then don't add these dependencies.
      return
    }

    val implementationDependencies =
      setOf(
        "$APP_PLATFORM_GROUP:common-public:$APP_PLATFORM_VERSION",
        "$APP_PLATFORM_GROUP:presenter-public:$APP_PLATFORM_VERSION",
        "$APP_PLATFORM_GROUP:renderer-public:$APP_PLATFORM_VERSION",
        "$APP_PLATFORM_GROUP:scope-public:$APP_PLATFORM_VERSION",
      )
    val testImplementationDependencies =
      setOf("$APP_PLATFORM_GROUP:scope-testing:$APP_PLATFORM_VERSION")
    val robotDependencies = setOf("$APP_PLATFORM_GROUP:robot-public:$APP_PLATFORM_VERSION")

    plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementationDependencies.forEach { dep -> implementation(dep) }
        if (isRobotsModule()) {
          robotDependencies.forEach { dep -> implementation(dep) }
        }
      }
      testingSourceSets.forEach { sourceSetName ->
        kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
          testImplementationDependencies.forEach { dep -> implementation(dep) }
        }
      }
    }

    withJvmOrAndroidPlugin {
      implementationDependencies.forEach { dep -> dependencies.add("implementation", dep) }
      testingSourceSets.forEach { sourceSetName ->
        testImplementationDependencies.forEach { dep -> dependencies.add(sourceSetName, dep) }
      }
      if (isRobotsModule()) {
        robotDependencies.forEach { dep -> dependencies.add("implementation", dep) }
      }
    }

    plugins.withId(PluginIds.ANDROID_KMP_LIBRARY) {
      dependencies.add(
        "androidMainImplementation",
        "$APP_PLATFORM_GROUP:renderer-android-view-public:$APP_PLATFORM_VERSION",
      )
    }

    plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
      dependencies.add(
        "implementation",
        "$APP_PLATFORM_GROUP:renderer-android-view-public:$APP_PLATFORM_VERSION",
      )

      if (isAppModule()) {
        robotDependencies.forEach { dep -> dependencies.add("androidTestImplementation", dep) }
      }
    }
  }

  private fun Project.addImplDependencies() {
    if (!appPlatform.isAddImplModuleDependencies().get()) {
      // If disabled, then don't add these dependencies.
      return
    }

    val implementationDependencies = buildSet {
      if (appPlatform.isMoleculeEnabled().get()) {
        add("$APP_PLATFORM_GROUP:presenter-molecule-impl:$APP_PLATFORM_VERSION")
      }
      if (appPlatform.isKotlinInjectEnabled().get()) {
        add("$APP_PLATFORM_GROUP:kotlin-inject-impl:$APP_PLATFORM_VERSION")
      }
      if (appPlatform.isMetroEnabled().get()) {
        add("$APP_PLATFORM_GROUP:metro-impl:$APP_PLATFORM_VERSION")
      }
    }

    plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
      kmpExtension.sourceSets.getByName("commonMain").dependencies {
        implementationDependencies.forEach { dep -> implementation(dep) }
      }
    }

    withJvmOrAndroidPlugin {
      implementationDependencies.forEach { dep -> dependencies.add("implementation", dep) }
    }
  }

  public companion object {
    /**
     * Returns the set of dependencies that need to be exported in a Framework for native targets in
     * order to make App Platform work.
     */
    @JvmStatic
    public fun exportedDependencies(): Set<String> =
      setOf(
          "common-public",
          "di-common-public",
          "kotlin-inject-contribute-public",
          "kotlin-inject-impl",
          "kotlin-inject-public",
          "metro-impl",
          "metro-public",
          "presenter-backstack-nav3-public",
          "presenter-molecule-impl",
          "presenter-molecule-public",
          "presenter-public",
          "renderer-compose-multiplatform-public",
          "renderer-public",
          "scope-public",
        )
        .mapTo(mutableSetOf()) { "$APP_PLATFORM_GROUP:$it:$APP_PLATFORM_VERSION" }
  }
}
