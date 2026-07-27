package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testfixtures.ProjectBuilder
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

class ModuleStructurePluginTest {

  @Test
  fun `module structure dependency checks are enabled by default`() {
    val project = createImplModule()

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    val task = project.dependencyCheckTask()
    assertThat(task.enabled).isTrue()
    assertThat(task.onlyIf.isSatisfiedBy(task)).isTrue()
    project.assertHasPublicModuleDependency()
  }

  @Test
  fun `module structure can be enabled from a Kotlin plugin callback`() {
    val project = createImplModule()

    project.plugins.withId(PluginIds.KOTLIN_JVM) {
      project.appPlatform.enableModuleStructure(true)
    }
    project.evaluate()

    val task = project.dependencyCheckTask()
    assertThat(task.enabled).isTrue()
    assertThat(task.onlyIf.isSatisfiedBy(task)).isTrue()
    project.assertHasPublicModuleDependency()
  }

  @Test
  fun `disabled dependency checks are skipped and default module dependencies remain`() {
    val project = createImplModule()
    project.dependencies.add("compileClasspath", "com.example:forbidden-impl:1.0")

    project.appPlatform.enableModuleStructure(
      Action { options -> options.enableDependencyCheck(false) }
    )
    project.evaluate()

    val task = project.dependencyCheckTask()
    assertThat(project.tasks.names).contains("checkModuleStructureDependenciesJvm")
    assertThat(project.tasks.names).contains("checkModuleStructureDependencies")
    assertThat(task.onlyIf.isSatisfiedBy(task)).isFalse()
    project.assertHasPublicModuleDependency()
  }

  @Test
  fun `dependency checks can be disabled after module structure was enabled`() {
    val project = createImplModule()

    project.appPlatform.enableModuleStructure(true)
    project.appPlatform.enableModuleStructure(
      Action { options -> options.enableDependencyCheck(false) }
    )
    project.evaluate()

    val task = project.dependencyCheckTask()
    assertThat(project.tasks.names).contains("checkModuleStructureDependenciesJvm")
    assertThat(project.tasks.names).contains("checkModuleStructureDependencies")
    assertThat(task.onlyIf.isSatisfiedBy(task)).isFalse()
    project.assertHasPublicModuleDependency()
  }

  @Test
  fun `the lifecycle check task depends on module structure checks`() {
    val project = createImplModule()
    project.plugins.apply(LifecycleBasePlugin::class.java)

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    val checkTask = project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).get()
    val moduleStructureCheckTask = project.tasks.named("checkModuleStructureDependencies").get()

    assertThat(checkTask.taskDependencies.getDependencies(checkTask))
      .contains(moduleStructureCheckTask)
    project.assertHasPublicModuleDependency()
  }

  private fun createImplModule(): Project {
    val rootProject = ProjectBuilder.builder().withName("root").build()
    val libraryProject =
      ProjectBuilder.builder().withName("library").withParent(rootProject).build()
    ProjectBuilder.builder().withName("public").withParent(libraryProject).build()

    val project = ProjectBuilder.builder().withName("impl").withParent(libraryProject).build()
    project.plugins.apply(PluginIds.KOTLIN_JVM)
    project.configurations.maybeCreate("compileClasspath")
    project.plugins.apply(AppPlatformPlugin::class.java)
    return project
  }

  private fun Project.dependencyCheckTask(): ModuleStructureDependencyCheckTask =
    tasks
      .named("checkModuleStructureDependenciesJvm", ModuleStructureDependencyCheckTask::class.java)
      .get()

  private fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
  }

  private fun Project.assertHasPublicModuleDependency() {
    val projectDependencies =
      configurations.getByName("api").dependencies.filterIsInstance<ProjectDependency>().map {
        dependency ->
        dependency.path
      }

    assertThat(projectDependencies).contains(":library:public")
  }
}
