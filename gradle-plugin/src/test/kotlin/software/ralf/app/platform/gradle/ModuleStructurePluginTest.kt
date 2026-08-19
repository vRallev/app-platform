package software.ralf.app.platform.gradle

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlin.test.Test
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.Property
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testfixtures.ProjectBuilder
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

class ModuleStructurePluginTest {

  @Test
  fun `Gradle defaults archive names to the project name`() {
    val project = createImplModule()

    project.plugins.apply("base")

    assertThat(project.archivesName.get()).isEqualTo("impl")
  }

  @Test
  fun `module structure derives archive names from artifact IDs`() {
    val project = createImplModule()
    project.plugins.apply("base")

    project.appPlatform.enableModuleStructure(true)

    assertThat(project.archivesName.get()).isEqualTo("library-impl")
  }

  @Test
  fun `module structure preserves archive names configured before it is enabled`() {
    val project = createImplModule()
    project.plugins.apply("base")
    project.archivesName.set("custom-archive")

    project.appPlatform.enableModuleStructure(true)

    assertThat(project.archivesName.get()).isEqualTo("custom-archive")
  }

  @Test
  fun `module structure allows archive names to be configured after it is enabled`() {
    val project = createImplModule()
    project.plugins.apply("base")
    project.appPlatform.enableModuleStructure(true)

    project.archivesName.set("custom-archive")

    assertThat(project.archivesName.get()).isEqualTo("custom-archive")
  }

  @Test
  fun `module structure configures archive names when the base plugin is applied later`() {
    val project = createImplModule()
    project.appPlatform.enableModuleStructure(true)

    project.plugins.apply("base")

    assertThat(project.archivesName.get()).isEqualTo("library-impl")
  }

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

  @Test
  fun `test fixture checks are included in module structure checks`() {
    val project = createPublicModule()
    project.plugins.apply("java-test-fixtures")

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    val moduleStructureCheckTask = project.tasks.named("checkModuleStructureDependencies").get()
    val testFixturesCheckTask = project.testFixturesDependencyCheckTask()

    assertThat(moduleStructureCheckTask.taskDependencies.getDependencies(moduleStructureCheckTask))
      .contains(testFixturesCheckTask)
  }

  @Test
  fun `public test fixtures cannot depend on an impl module`() {
    val project = createPublicModule()
    project.plugins.apply("java-test-fixtures")
    project.dependencies.add("testFixturesImplementation", project.project(":library:impl"))

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    val task = project.testFixturesDependencyCheckTask()
    assertFailure { task.checkDependencies() }.isInstanceOf<GradleException>()
  }

  @Test
  fun `public test fixture api dependencies cannot include an impl module`() {
    val project = createPublicModule()
    project.plugins.apply("java-test-fixtures")
    project.dependencies.add("testFixturesApi", project.project(":library:impl"))

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    val task = project.testFixturesDependencyCheckTask()
    assertFailure { task.checkDependencies() }.isInstanceOf<GradleException>()
  }

  @Test
  fun `public test fixtures can depend on testing modules`() {
    val project = createPublicModule()
    project.plugins.apply("java-test-fixtures")
    project.dependencies.add("testFixturesImplementation", project.project(":library:testing"))

    project.appPlatform.enableModuleStructure(true)
    project.evaluate()

    project.testFixturesDependencyCheckTask().checkDependencies()
  }

  @Test
  fun `test fixtures can be enabled after module structure`() {
    val project = createPublicModule()
    project.appPlatform.enableModuleStructure(true)

    project.plugins.apply("java-test-fixtures")
    project.evaluate()

    assertThat(project.tasks.names).contains("checkModuleStructureDependenciesJvmTestFixtures")
  }

  @Test
  fun `disabled dependency checks also skip test fixture checks`() {
    val project = createPublicModule()
    project.plugins.apply("java-test-fixtures")

    project.appPlatform.enableModuleStructure { options -> options.enableDependencyCheck(false) }
    project.evaluate()

    val task = project.testFixturesDependencyCheckTask()
    assertThat(task.onlyIf.isSatisfiedBy(task)).isFalse()
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

  private fun createPublicModule(): Project {
    val rootProject = ProjectBuilder.builder().withName("root").build()
    val libraryProject =
      ProjectBuilder.builder().withName("library").withParent(rootProject).build()
    ProjectBuilder.builder().withName("impl").withParent(libraryProject).build()
    ProjectBuilder.builder().withName("testing").withParent(libraryProject).build()

    val project = ProjectBuilder.builder().withName("public").withParent(libraryProject).build()
    project.plugins.apply(PluginIds.KOTLIN_JVM)
    project.configurations.maybeCreate("compileClasspath")
    project.plugins.apply(AppPlatformPlugin::class.java)
    return project
  }

  private val Project.archivesName: Property<String>
    get() = extensions.getByType(BasePluginExtension::class.java).archivesName

  private fun Project.dependencyCheckTask(): ModuleStructureDependencyCheckTask =
    tasks
      .named("checkModuleStructureDependenciesJvm", ModuleStructureDependencyCheckTask::class.java)
      .get()

  private fun Project.testFixturesDependencyCheckTask(): ModuleStructureDependencyCheckTask =
    tasks
      .named(
        "checkModuleStructureDependenciesJvmTestFixtures",
        ModuleStructureDependencyCheckTask::class.java,
      )
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
