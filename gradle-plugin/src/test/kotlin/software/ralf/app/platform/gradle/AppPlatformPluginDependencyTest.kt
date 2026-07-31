package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import gradle_plugin.BuildConfig.APP_PLATFORM_GROUP
import gradle_plugin.BuildConfig.APP_PLATFORM_VERSION
import kotlin.test.Test
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

class AppPlatformPluginDependencyTest {

  @Test
  fun `AGP built-in Kotlin Android application receives Metro public and impl dependencies`() {
    val project = createProject(name = "app")
    project.plugins.apply(PluginIds.ANDROID_APP)
    project.plugins.apply(AppPlatformPlugin::class.java)

    project.appPlatform.enableMetro(true)
    project.appPlatform.addImplModuleDependencies(true)

    project.evaluate()

    project.assertHasDependency("implementation", appPlatformDependency("di-common-public"))
    project.assertHasDependency("implementation", appPlatformDependency("metro-public"))
    project.assertHasDependency("implementation", appPlatformDependency("metro-impl"))
    project.assertHasDependency(
      agpBuiltInKotlinDebugCompilerPluginConfiguration,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
    project.assertDoesNotHaveDependency(
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
    assertThat(project.plugins.hasPlugin(PluginIds.KOTLIN_ANDROID)).isFalse()
  }

  @Test
  fun `AGP built-in Kotlin Android library receives Metro public dependencies`() {
    val project = createProject(name = "impl")
    project.plugins.apply(PluginIds.ANDROID_LIBRARY)
    project.plugins.apply(AppPlatformPlugin::class.java)

    project.appPlatform.enableMetro(true)

    project.evaluate()

    project.assertHasDependency("implementation", appPlatformDependency("di-common-public"))
    project.assertHasDependency("implementation", appPlatformDependency("metro-public"))
    project.assertHasDependency(
      agpBuiltInKotlinDebugCompilerPluginConfiguration,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
    project.assertDoesNotHaveDependency(
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
    project.assertDoesNotHaveDependency("implementation", appPlatformDependency("metro-impl"))
    assertThat(project.plugins.hasPlugin(PluginIds.KOTLIN_ANDROID)).isFalse()
  }

  @Test
  fun `KMP Metro dependency wiring still works`() {
    val project = createProject(name = "impl")
    project.plugins.apply(PluginIds.KOTLIN_MULTIPLATFORM)
    project.plugins.apply(AppPlatformPlugin::class.java)
    project.appPlatform.enableMetro(true)
    project.appPlatform.addImplModuleDependencies(true)

    project.evaluate()

    project.assertHasDependency(
      "commonMainImplementation",
      appPlatformDependency("di-common-public"),
    )
    project.assertHasDependency("commonMainImplementation", appPlatformDependency("metro-public"))
    project.assertHasDependency("commonMainImplementation", appPlatformDependency("metro-impl"))
    project.assertHasDependency(
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
  }

  @Test
  fun `legacy JVM Metro dependency wiring still works`() {
    val project = createProject(name = "impl")
    project.plugins.apply(PluginIds.KOTLIN_JVM)
    project.plugins.apply(AppPlatformPlugin::class.java)

    project.appPlatform.enableMetro(true)
    project.appPlatform.addImplModuleDependencies(true)

    project.evaluate()

    project.assertHasDependency("implementation", appPlatformDependency("di-common-public"))
    project.assertHasDependency("implementation", appPlatformDependency("metro-public"))
    project.assertHasDependency("implementation", appPlatformDependency("metro-impl"))
    project.assertHasDependency(
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
  }

  @Test
  fun `legacy Kotlin Android and Android application plugins do not duplicate Metro dependencies`() {
    val project = createProject(name = "app")
    project.plugins.apply(PluginIds.ANDROID_APP)
    project.plugins.apply(PluginIds.KOTLIN_ANDROID)
    project.plugins.apply(AppPlatformPlugin::class.java)

    project.appPlatform.enableMetro(true)
    project.appPlatform.addImplModuleDependencies(true)

    project.evaluate()

    project.assertHasDependencyOnce("implementation", appPlatformDependency("di-common-public"))
    project.assertHasDependencyOnce("implementation", appPlatformDependency("metro-public"))
    project.assertHasDependencyOnce("implementation", appPlatformDependency("metro-impl"))
    project.assertHasDependencyOnce(
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
    project.assertDoesNotHaveDependency(
      agpBuiltInKotlinDebugCompilerPluginConfiguration,
      appPlatformDependency("metro-contribute-impl-compiler-plugin"),
    )
  }

  private fun createProject(name: String): Project {
    val rootProject = ProjectBuilder.builder().withName("root").build()
    return ProjectBuilder.builder().withName(name).withParent(rootProject).build()
  }

  private fun appPlatformDependency(artifactId: String): String =
    "$APP_PLATFORM_GROUP:$artifactId:$APP_PLATFORM_VERSION"

  private val agpBuiltInKotlinDebugCompilerPluginConfiguration =
    "${PLUGIN_CLASSPATH_CONFIGURATION_NAME}Debug"

  private fun Project.assertHasDependency(configurationName: String, coordinate: String) {
    val dependencies = dependencyCoordinates(configurationName)
    assertThat(dependencies).contains(coordinate)
  }

  private fun Project.assertDoesNotHaveDependency(configurationName: String, coordinate: String) {
    val dependencies = dependencyCoordinates(configurationName)
    assertThat(dependencies).doesNotContain(coordinate)
  }

  private fun Project.assertHasDependencyOnce(configurationName: String, coordinate: String) {
    val dependencies = dependencyCoordinates(configurationName)
    assertThat(dependencies.count { dependency -> dependency == coordinate }).isEqualTo(1)
  }

  private fun Project.dependencyCoordinates(configurationName: String): List<String> =
    configurations
      .getByName(configurationName)
      .dependencies
      .filterIsInstance<ExternalModuleDependency>()
      .map { dependency -> "${dependency.group}:${dependency.name}:${dependency.version}" }

  private fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
  }
}
