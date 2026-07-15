package software.ralf.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import software.ralf.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformBuildSrc

public open class JvmLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(Plugins.KOTLIN_JVM)

    target.configureKotlin()
    target.configureTests()
    target.configureCoroutines()
  }

  private fun Project.configureKotlin() {
    dependencies.add(
      "api",
      dependencies.platform(libs.findLibrary("kotlin.bom").get().get().toString()),
    )

    extensions.getByType(KotlinJvmProjectExtension::class.java).compilerOptions {
      allWarningsAsErrors.set(appPlatformBuildSrc.isKotlinWarningsAsErrors())

      jvmTarget.set(javaTarget)
    }

    with(extensions.getByType(JavaPluginExtension::class.java)) {
      sourceCompatibility = javaVersion
      targetCompatibility = javaVersion
    }
  }

  private fun Project.configureTests() {
    releaseTask.configure { task -> task.dependsOn("test") }

    dependencies.add("testImplementation", libs.findLibrary("kotlin.test").get().get().toString())
    dependencies.add("testImplementation", libs.findLibrary("assertk").get().get().toString())
  }

  private fun Project.configureCoroutines() {
    dependencies.add("implementation", libs.findLibrary("coroutines.core").get().get().toString())
    dependencies.add(
      "testImplementation",
      libs.findLibrary("coroutines.test").get().get().toString(),
    )
    dependencies.add("testImplementation", libs.findLibrary("turbine").get().get().toString())
  }
}
