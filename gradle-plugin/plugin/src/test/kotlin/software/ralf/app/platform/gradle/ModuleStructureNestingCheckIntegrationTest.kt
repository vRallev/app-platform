package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import assertk.assertions.isNull
import java.io.File
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir

class ModuleStructureNestingCheckIntegrationTest {
  @TempDir lateinit var projectDir: File

  @Test
  fun `separate tasks check nesting and dependencies with isolated projects and cache reuse`() {
    fixture(":abc:public", ":abc:impl", ":features:def:public", ":tools")
    module(":abc:public")
    module(":abc:impl")
    module(":features:def:public")

    repeat(2) { run ->
      val result = runner("checkModuleStructureDependencies", "checkModuleStructureNesting").build()

      listOf(":abc:public", ":abc:impl", ":features:def:public").forEach { path ->
        assertThat(result.task("$path:checkModuleStructureDependenciesFixture")?.outcome)
          .isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
      }
      assertThat(result.task(":checkModuleStructureDependencies")).isNull()
      assertThat(result.task(":checkModuleStructureNesting")?.outcome)
        .isEqualTo(TaskOutcome.SUCCESS)
      if (run == 1) assertThat(result.output).contains("Reusing configuration cache.")
    }
  }

  @Test
  fun `root lifecycle check rejects nesting without module structure conventions`() {
    fixture(":abc:public", ":abc:def:public")
    File(projectDir, "build.gradle").appendText("\napply plugin: 'base'\n")

    val result = runner("check").buildAndFail()

    assertThat(result.output).contains(":abc contains :abc:def:public")
  }

  @Test
  fun `Groovy lists and varargs add exact exceptions after enabling nesting validation`() {
    fixture(":abc:public", ":abc:def:public", ":legacy:impl", ":legacy:child:testing")
    File(projectDir, "build.gradle")
      .appendText(
        """

        appPlatform.enableModuleStructureNestingCheck {
          allowNestedLibrariesIn ':abc'
          allowNestedLibrariesIn([':legacy'])
        }
        """
          .trimIndent()
      )

    runner("checkModuleStructureNesting").build()
  }

  @Test
  fun `Kotlin lists and varargs add exact exceptions`() {
    fixture(":abc:public", ":abc:def:public", ":legacy:impl", ":legacy:child:testing")
    File(projectDir, "build.gradle").delete()
    File(projectDir, "build.gradle.kts")
      .writeText(
        """
        plugins { id("software.ralf.app.platform") }
        appPlatform {
          enableModuleStructureNestingCheck {
            allowNestedLibrariesIn(listOf(":abc"))
            allowNestedLibrariesIn(":legacy", ":abc")
          }
        }
        """
          .trimIndent()
      )

    runner("checkModuleStructureNesting").build()
  }

  @Test
  fun `disabling nesting still enforces module dependency rules`() {
    fixture(":abc:public", ":abc:def:public", options = "enableLibraryNestingCheck false")
    module(":abc:public", dependency = ":other:impl")

    val result =
      runner("checkModuleStructureDependencies", "checkModuleStructureNesting").buildAndFail()

    assertThat(result.output)
      .contains(":public modules are only allowed to depend on other :public modules.")
  }

  @Test
  fun `nesting can be disabled through the root DSL`() {
    fixture(":abc:public", ":abc:def:public", options = "enableLibraryNestingCheck false")

    runner("checkModuleStructureNesting").build()
  }

  @Test
  fun `nesting check does not run module dependency checks`() {
    fixture(":abc:public")
    module(":abc:public", dependency = ":other:impl")

    val result = runner("checkModuleStructureNesting").build()

    assertThat(result.task(":checkModuleStructureNesting")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":abc:public:checkModuleStructureDependenciesFixture")).isNull()
  }

  @Test
  fun `folder check ignores sibling projects and reuses the configuration cache`() {
    fixture(
      ":features:abc:public",
      ":features:abc:impl",
      ":other:public",
      ":other:child:public",
      checkProject = ":features",
    )

    repeat(2) { run ->
      val result = runner(":features:checkModuleStructureNesting").build()

      assertThat(result.task(":features:checkModuleStructureNesting")?.outcome)
        .isEqualTo(TaskOutcome.SUCCESS)
      assertThat(result.task(":checkModuleStructureNesting")).isNull()
      if (run == 1) assertThat(result.output).contains("Reusing configuration cache.")
    }
  }

  @Test
  fun `folder check rejects nested libraries within its subtree`() {
    fixture(":features:abc:public", ":features:abc:def:public", checkProject = ":features")

    val result = runner(":features:checkModuleStructureNesting").buildAndFail()

    assertThat(result.output).contains(":features:abc contains :features:abc:def:public")
  }

  @Test
  fun `folder check includes the folder itself as a library boundary`() {
    fixture(":features:public", ":features:child:public", checkProject = ":features")

    val result = runner(":features:checkModuleStructureNesting").buildAndFail()

    assertThat(result.output).contains(":features contains :features:child:public")
  }

  @Test
  fun `folder exceptions cannot refer to libraries outside the checked subtree`() {
    fixture(
      ":features:abc:public",
      ":other:public",
      checkProject = ":features",
      options = "allowNestedLibrariesIn ':other'",
    )

    val result = runner(":features:checkModuleStructureNesting").buildAndFail()

    assertThat(result.output).contains("libraries in the checked subtree: :other.")
  }

  @Test
  fun `sibling folder checks use independent exceptions`() {
    fixture(
      ":features:legacy:public",
      ":features:legacy:child:public",
      ":recipes:legacy:public",
      ":recipes:legacy:child:public",
      checkProject = ":features",
      options = "allowNestedLibrariesIn ':features:legacy'",
    )
    nestingCheck(":recipes")

    val result = runner("checkModuleStructureNesting", "--continue").buildAndFail()

    assertThat(result.task(":features:checkModuleStructureNesting")?.outcome)
      .isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":recipes:checkModuleStructureNesting")?.outcome)
      .isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output).contains(":recipes:legacy contains :recipes:legacy:child:public")
  }

  @Test
  fun `folder exceptions do not override an ancestor check`() {
    fixture(":features:legacy:public", ":features:legacy:child:public")
    nestingCheck(":features", options = "allowNestedLibrariesIn ':features:legacy'")

    runner(":features:checkModuleStructureNesting").build()
    val result = runner(":checkModuleStructureNesting").buildAndFail()

    assertThat(result.output).contains(":features:legacy contains :features:legacy:child:public")
  }

  @Test
  fun `nesting exceptions are unavailable in the module structure DSL`() {
    fixture(":abc:public")
    File(projectDir, "abc/public/build.gradle.kts")
      .writeText(
        """
        plugins { id("software.ralf.app.platform") }
        appPlatform.enableModuleStructure {
          allowNestedLibrariesIn(":abc")
        }
        """
          .trimIndent()
      )

    val result = runner(":abc:public:checkModuleStructureDependencies").buildAndFail()

    assertThat(result.output).contains("Unresolved reference")
    assertThat(result.output).contains("allowNestedLibrariesIn")
  }

  private fun fixture(vararg paths: String, checkProject: String = ":", options: String = "") {
    paths.forEach { File(projectDir, it.drop(1).replace(':', '/')).mkdirs() }
    File(projectDir, "settings.gradle")
      .writeText("rootProject.name = 'fixture'\n" + paths.joinToString("\n") { "include '$it'" })
    File(projectDir, "build.gradle").writeText("")
    nestingCheck(checkProject, options)
  }

  private fun nestingCheck(path: String, options: String = "") {
    val directory = File(projectDir, path.drop(1).replace(':', '/'))
    directory.mkdirs()
    File(directory, "build.gradle")
      .writeText(
        """
        plugins { id 'software.ralf.app.platform' }
        appPlatform.enableModuleStructureNestingCheck {
          $options
        }
        """
          .trimIndent()
      )
  }

  private fun module(path: String, dependency: String = ":other:public") {
    File(projectDir, "${path.drop(1).replace(':', '/')}/build.gradle")
      .writeText(
        """
        import software.ralf.app.platform.gradle.ModuleStructureDependencyCheckTask

        plugins { id 'software.ralf.app.platform' }
        appPlatform.enableModuleStructure true

        tasks.register('checkModuleStructureDependenciesFixture', ModuleStructureDependencyCheckTask) {
          modulePath = '$path'
          moduleCompileClasspath = ['$dependency'] as Set
        }
        tasks.named('checkModuleStructureDependencies') {
          dependsOn 'checkModuleStructureDependenciesFixture'
        }
        """
          .trimIndent()
      )
  }

  // Fixtures apply only App Platform and core Gradle plugins, so no external plugin versions can
  // be shadowed by the injected classpath.
  @Suppress("WithPluginClasspathUsage")
  private fun runner(vararg tasks: String): GradleRunner =
    GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(tasks.toList() + listOf("--isolated-projects", "--stacktrace"))
}
