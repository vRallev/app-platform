package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformBuildSrc
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformGradlePlugin

public open class AndroidAppPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.ANDROID_APP)
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(BaseAndroidPlugin::class.java)

    target.configureAndroidApp()
    target.configureKotlin()
    target.configureCoroutines()
    target.configureDetekt()
  }

  private fun Project.configureAndroidApp() {
    android.defaultConfig.minSdk = 26

    androidComponents.beforeVariants { variant ->
      if (variant.buildType != "debug") {
        variant.enable = false
      }
    }

    appPlatformGradlePlugin.enableModuleStructure(true)
    releaseTask.configure { it.dependsOn("checkModuleStructureDependencies") }

    // Android test runtimes inherit strict versions from the app runtime. Align this dependency
    // with the newer version brought in by the shared robot modules.
    dependencies.constraints.add(
      "implementation",
      libs.findLibrary("androidx.concurrent.futures").get().get().toString(),
    )
  }

  private fun Project.configureKotlin() {
    extensions.getByType(KotlinAndroidProjectExtension::class.java).compilerOptions {
      allWarningsAsErrors.set(appPlatformBuildSrc.isKotlinWarningsAsErrors())
      jvmTarget.set(javaTarget)
    }
  }

  private fun Project.configureCoroutines() {
    dependencies.add("implementation", libs.findLibrary("coroutines.core").get().get().toString())
  }
}
