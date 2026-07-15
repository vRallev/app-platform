package software.ralf.app.platform.gradle

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

class ModuleStructureDependencyCheckTaskTest {

  @Test
  fun `impl dependency within the same library is forbidden by default`() {
    assertFailure {
      checkDependencies(
        modulePath = ":library:impl-specific",
        moduleCompileClasspath = setOf(":library:impl-common"),
      )
    }
      .isInstanceOf<GradleException>()
  }

  @Test
  fun `impl dependency within the same library can be allowed`() {
    checkDependencies(
      modulePath = ":library:impl-specific",
      moduleCompileClasspath = setOf(":library:impl-common"),
      allowLibraryImplToImplDependencies = true,
    )
  }

  @Test
  fun `impl dependency from another library remains forbidden`() {
    assertFailure {
      checkDependencies(
        modulePath = ":library:impl-specific",
        moduleCompileClasspath = setOf(":other-library:impl-common"),
        allowLibraryImplToImplDependencies = true,
      )
    }
      .isInstanceOf<GradleException>()
  }

  @Test
  fun `external impl dependency remains forbidden`() {
    assertFailure {
      checkDependencies(
        modulePath = ":library:impl-specific",
        moduleCompileClasspath = setOf("com.example:library-impl-common:1.0"),
        allowLibraryImplToImplDependencies = true,
      )
    }
      .isInstanceOf<GradleException>()
  }

  @Test
  fun `non-impl module cannot use the option to import an impl module`() {
    assertFailure {
      checkDependencies(
        modulePath = ":library:internal",
        moduleCompileClasspath = setOf(":library:impl-common"),
        allowLibraryImplToImplDependencies = true,
      )
    }
      .isInstanceOf<GradleException>()
  }

  @Test
  fun `app modules can import impl modules`() {
    listOf(
        ":app",
        ":app-mobile",
        ":feature:app:feature",
        ":apps:feature",
        ":app-mobile:feature",
        ":feature:app-mobile:feature",
        ":apps-mobile:feature",
        ":feature:apps-mobile:feature",
      )
      .forEach { modulePath ->
        checkDependencies(
          modulePath = modulePath,
          moduleCompileClasspath = setOf(":library:impl-common"),
        )
      }
  }

  @Test
  fun `internal dependency within the same library remains allowed`() {
    checkDependencies(
      modulePath = ":library:impl",
      moduleCompileClasspath = setOf(":library:internal"),
    )
  }

  @Test
  fun `internal dependency from another library remains forbidden`() {
    assertFailure {
      checkDependencies(
        modulePath = ":library:impl",
        moduleCompileClasspath = setOf(":other-library:internal"),
      )
    }
      .isInstanceOf<GradleException>()
  }

  private fun checkDependencies(
    modulePath: String,
    moduleCompileClasspath: Set<String>,
    allowLibraryImplToImplDependencies: Boolean? = null,
  ) {
    val project = ProjectBuilder.builder().build()
    val task =
      project.tasks
        .register(
          "checkModuleStructureDependencies",
          ModuleStructureDependencyCheckTask::class.java,
        )
        .get()

    task.modulePath = modulePath
    task.moduleCompileClasspath = moduleCompileClasspath
    allowLibraryImplToImplDependencies?.let(task.allowLibraryImplToImplDependencies::set)
    task.checkDependencies()
  }
}
