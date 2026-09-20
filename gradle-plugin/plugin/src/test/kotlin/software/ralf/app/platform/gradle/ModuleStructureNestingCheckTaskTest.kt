package software.ralf.app.platform.gradle

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasMessage
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

class ModuleStructureNestingCheckTaskTest {
  @Test
  fun `sibling libraries and grouping projects are allowed`() {
    task(
        ":abc:public",
        ":abc:impl",
        ":def:public",
        ":features:login:public",
        ":features:user:impl",
        ":sample:app:android",
        ":internal:testing",
      )
      .checkLibraryNesting()
  }

  @Test
  fun `nesting is rejected by default with all offending paths`() {
    val task = task(":abc:public", ":abc:impl", ":abc:def:public", ":abc:ghi:impl")

    assertFailure { task.checkLibraryNesting() }
      .hasMessage(
        "Library modules must be direct children of their library:\n" +
          " - :abc contains :abc:def:public, :abc:ghi:impl\n" +
          "Move nested projects outside the library, or configure allowNestedLibrariesIn " +
          "in the appPlatform.enableModuleStructureNestingCheck block of the project running this check."
      )
  }

  @Test
  fun `all library module types establish a library boundary`() {
    listOf(
        "impl",
        "impl-vendor",
        "internal",
        "testing",
        "public-robots",
        "impl-robots",
        "internal-robots",
      )
      .forEach { moduleName ->
        val task = task(":abc:$moduleName", ":abc:def:public")

        assertFailure { task.checkLibraryNesting() }
      }
  }

  @Test
  fun `projects cannot be nested inside a library module`() {
    val task = task(":abc:public", ":abc:public:tools")

    assertFailure { task.checkLibraryNesting() }
  }

  @Test
  fun `library path matching respects segment boundaries`() {
    task(":abc:public", ":abcd:def:public").checkLibraryNesting()
  }

  @Test
  fun `nesting check can be disabled`() {
    val task = task(":abc:public", ":abc:def:public")
    task.enableLibraryNestingCheck.set(false)

    task.checkLibraryNesting()
  }

  @Test
  fun `exact containing library exceptions allow nesting`() {
    val task = task(":abc:public", ":abc:def:public")
    task.allowedNestedLibraryPaths.add(":abc")

    task.checkLibraryNesting()
  }

  @Test
  fun `exceptions do not exempt nested libraries from their own checks`() {
    val task = task(":abc:public", ":abc:def:public", ":abc:def:ghi:public")
    task.allowedNestedLibraryPaths.add(":abc")

    assertFailure { task.checkLibraryNesting() }

    task.allowedNestedLibraryPaths.add(":abc:def")
    task.checkLibraryNesting()
  }

  @Test
  fun `exceptions do not exempt other libraries`() {
    val task = task(":abc:public", ":abc:def:public", ":xyz:public", ":xyz:def:public")
    task.allowedNestedLibraryPaths.add(":abc")

    assertFailure { task.checkLibraryNesting() }
  }

  @Test
  fun `exceptions must identify existing libraries`() {
    listOf(":missing", "abc", ":abc:", ":abc:*", ":abc:public", ":features", ":").forEach { path ->
      val task = task(":abc:public", ":features:def:public")
      task.allowedNestedLibraryPaths.add(path)

      assertFailure { task.checkLibraryNesting() }
        .hasMessage(
          "allowNestedLibrariesIn requires exact library paths. These paths do not identify " +
            "libraries in the checked subtree: $path."
        )
    }
  }

  @Test
  fun `nesting check registers without adding module conventions or lifecycle tasks`() {
    val root = ProjectBuilder.builder().build()
    root.plugins.apply(AppPlatformPlugin::class.java)

    root.appPlatform.enableModuleStructureNestingCheck(true)

    val task = root.tasks.named("checkModuleStructureNesting").get()
    assertThat(task is ModuleStructureNestingCheckTask).isTrue()
    assertThat(root.tasks.names).doesNotContain("checkModuleStructureDependencies")
    assertThat(root.tasks.names).doesNotContain("check")
    assertThat(root.plugins.hasPlugin("base")).isFalse()
    assertThat(root.plugins.hasPlugin(ModuleStructurePlugin::class.java)).isFalse()
    assertThat(root.appPlatform.isModuleStructureEnabled().get()).isFalse()
  }

  @Test
  fun `module conventions do not implicitly enable nesting validation`() {
    val root = ProjectBuilder.builder().build()
    val module = ProjectBuilder.builder().withName("public").withParent(root).build()
    module.plugins.apply(AppPlatformPlugin::class.java)

    module.appPlatform.enableModuleStructure(true)

    assertThat(module.tasks.names).contains("checkModuleStructureDependencies")
    assertThat(module.tasks.names).doesNotContain("checkModuleStructureNesting")
  }

  @Test
  fun `nesting check is not registered when disabled`() {
    val root = ProjectBuilder.builder().build()
    root.plugins.apply(AppPlatformPlugin::class.java)

    root.appPlatform.enableModuleStructureNestingCheck(false)

    assertThat(root.tasks.names).doesNotContain("checkModuleStructureNesting")
  }

  @Test
  fun `existing check task depends on nesting validation`() {
    val root = ProjectBuilder.builder().build()
    root.tasks.register("check")
    root.plugins.apply(AppPlatformPlugin::class.java)

    root.appPlatform.enableModuleStructureNestingCheck(true)

    val task = root.tasks.named("checkModuleStructureNesting").get()
    val check = root.tasks.named("check").get()
    assertThat(check.taskDependencies.getDependencies(check)).contains(task)
  }

  @Test
  fun `check task added later depends on nesting validation`() {
    val root = ProjectBuilder.builder().build()
    root.plugins.apply(AppPlatformPlugin::class.java)
    root.appPlatform.enableModuleStructureNestingCheck(true)

    root.tasks.register("check")

    val task = root.tasks.named("checkModuleStructureNesting").get()
    val check = root.tasks.named("check").get()
    assertThat(check.taskDependencies.getDependencies(check)).contains(task)
  }

  private fun task(vararg projectPaths: String): ModuleStructureNestingCheckTask =
    ProjectBuilder.builder()
      .build()
      .tasks
      .register("checkModuleStructureNesting", ModuleStructureNestingCheckTask::class.java)
      .get()
      .apply { this.projectPaths.set(projectPaths.toSet()) }
}
