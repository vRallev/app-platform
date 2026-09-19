package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import gradle_plugin.BuildConfig.ANDROIDX_CONCURRENT_FUTURES_VERSION
import java.io.File
import kotlin.test.Test
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

class AndroidComposeDependencyAlignmentTest {

  @Test
  fun `Compose app and instrumented tests resolve the required concurrent futures version`() {
    val project = createProject()
    project.dependencies.add("implementation", "androidx.concurrent:concurrent-futures:1.1.0")
    project.dependencies.add(
      "androidTestImplementation",
      "androidx.concurrent:concurrent-futures:$ANDROIDX_CONCURRENT_FUTURES_VERSION",
    )

    val appRuntime = project.runtime("appRuntime", "implementation")
    val testRuntime = project.runtime("testRuntime", "implementation", "androidTestImplementation")
    testRuntime.shouldResolveConsistentlyWith(appRuntime)

    assertThat(appRuntime.concurrentFuturesVersions())
      .isEqualTo(listOf(ANDROIDX_CONCURRENT_FUTURES_VERSION))
    assertThat(testRuntime.concurrentFuturesVersions())
      .isEqualTo(listOf(ANDROIDX_CONCURRENT_FUTURES_VERSION))
  }

  @Test
  fun `a newer app dependency is preserved in the app and instrumented tests`() {
    val project = createProject()
    project.dependencies.add("implementation", "androidx.concurrent:concurrent-futures:2.0.0")
    project.dependencies.add(
      "androidTestImplementation",
      "androidx.concurrent:concurrent-futures:$ANDROIDX_CONCURRENT_FUTURES_VERSION",
    )

    val appRuntime = project.runtime("appRuntime", "implementation")
    val testRuntime = project.runtime("testRuntime", "implementation", "androidTestImplementation")
    testRuntime.shouldResolveConsistentlyWith(appRuntime)

    assertThat(appRuntime.concurrentFuturesVersions()).isEqualTo(listOf("2.0.0"))
    assertThat(testRuntime.concurrentFuturesVersions()).isEqualTo(listOf("2.0.0"))
  }

  @Test
  fun `the constraint does not introduce a dependency when concurrent futures is unused`() {
    val project = createProject()

    assertThat(project.runtime("appRuntime", "implementation").concurrentFuturesVersions())
      .isEmpty()
  }

  @Test
  fun `apps without Compose and library modules do not receive the constraint`() {
    val app = createProject(composeEnabled = false)
    val library = createProject(name = "feature", pluginId = PluginIds.ANDROID_LIBRARY)

    assertThat(app.configurations.getByName("implementation").dependencyConstraints).isEmpty()
    assertThat(library.configurations.getByName("implementation").dependencyConstraints).isEmpty()
  }

  private fun createProject(
    name: String = "app",
    pluginId: String = PluginIds.ANDROID_APP,
    composeEnabled: Boolean = true,
  ): Project {
    val root = ProjectBuilder.builder().withName("root").build()
    val project = ProjectBuilder.builder().withName(name).withParent(root).build()
    project.plugins.apply(pluginId)
    project.plugins.apply(AppPlatformPlugin::class.java)
    project.appPlatform.enableComposeUi(composeEnabled)

    // Isolate the dependency being aligned from the Compose and robot dependencies.
    project.configurations.getByName("implementation").dependencies.clear()
    project.configurations.getByName("androidTestImplementation").dependencies.clear()

    val repository = File(project.projectDir, "repository")
    listOf("1.1.0", ANDROIDX_CONCURRENT_FUTURES_VERSION, "2.0.0").forEach { version ->
      val pom =
        File(
          repository,
          "androidx/concurrent/concurrent-futures/$version/concurrent-futures-$version.pom",
        )
      pom.parentFile.mkdirs()
      pom.writeText(
        """
        <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>androidx.concurrent</groupId>
          <artifactId>concurrent-futures</artifactId>
          <version>$version</version>
          <packaging>pom</packaging>
        </project>
        """
          .trimIndent()
      )
    }
    project.repositories.maven { it.url = repository.toURI() }
    return project
  }

  private fun Project.runtime(name: String, vararg parents: String): Configuration =
    configurations.create(name) { configuration ->
      parents.forEach { parent -> configuration.extendsFrom(configurations.getByName(parent)) }
    }

  private fun Configuration.concurrentFuturesVersions(): List<String> {
    // Resolve artifacts as well, so version conflicts fail instead of remaining unresolved results.
    incoming.files.files
    return incoming.resolutionResult.allComponents.mapNotNull { component ->
      component.moduleVersion
        ?.takeIf { it.group == "androidx.concurrent" && it.name == "concurrent-futures" }
        ?.version
    }
  }
}
