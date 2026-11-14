package software.ralf.app.platform.metro.compiler

import java.io.File
import java.net.URI
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class IncrementalCompilationTest {
  @TempDir lateinit var projectDir: File

  @Test
  fun `changed constructor qualifier updates the downstream robot graph`() {
    fixture("qualifier")
    baseline("before")

    replaceSource(module = "lib", old = "Named(\"before\")", new = "Named(\"after\")")

    rebuild(expected = "after", changedModules = listOf("lib", "app"))
  }

  @Test
  fun `adding and removing constructor parameters updates downstream providers`() {
    fixture("constructor")
    baseline("value")

    replaceSource(
      module = "lib",
      old = "val value: String",
      new = "value: String, count: Int",
    )
    replaceSource(
      module = "lib",
      old = ": Scoped",
      new = ": Scoped { val value = value + count }",
    )
    rebuild(expected = "value7", changedModules = listOf("lib", "app"))

    replaceSource(module = "lib", old = "value: String, count: Int", new = "val value: String")
    replaceSource(module = "lib", old = " { val value = value + count }", new = "")
    rebuild(expected = "value", changedModules = listOf("lib", "app"))
  }

  @Test
  fun `moving a contribution removes its old scope binding`() {
    fixture("scope")
    baseline("1,0")

    replaceSource(module = "lib", old = "FirstScope::class", new = "SecondScope::class")

    rebuild(expected = "0,1", changedModules = listOf("lib", "app"))
  }

  @Test
  fun `sealed model additions and removals update unchanged renderer and graph modules`() {
    fixture("sealedHierarchy")
    baseline("Branch,First,Model")

    replaceSource(
      module = "models",
      old = "data object First : Branch",
      new = "data object First : Branch\n    data object Second : Branch",
    )
    rebuild(expected = "Branch,First,Model,Second", changedModules = listOf("models", "lib", "app"))

    replaceSource(module = "models", old = "data object First : Branch", new = "")
    rebuild(expected = "Branch,Model,Second", changedModules = listOf("models", "lib", "app"))
  }

  private fun fixture(name: String) {
    val resources =
      File("metro-extensions/contribute/impl-compiler-plugin/src/test/resources/incremental/$name")
    val modules = resources.listFiles()!!.filter { it.isDirectory }.map { it.name }.sorted()
    for (module in modules) {
      File(resources, module).copyRecursively(File(projectDir, "$module/src/main/kotlin"))
    }
    File(projectDir, "settings.gradle")
      .writeText(
        """
      pluginManagement { repositories { mavenCentral() } }
      dependencyResolutionManagement { repositories { mavenCentral() } }
      rootProject.name = 'incremental-contributions'
      include ${modules.joinToString { "':$it'" }}
      """
          .trimIndent(),
      )
    val properties =
      Properties().apply {
        setProperty("kotlin.incremental", "true")
        setProperty("kotlin.daemon.useFallbackStrategy", "false")
        setProperty("kotlin.build.report.output", "file")
        setProperty("org.gradle.jvmargs", "-Xmx1g -Dfile.encoding=UTF-8")
        setProperty("kotlin.daemon.jvmargs", "-Xmx1g")
        setProperty("org.gradle.workers.max", "2")
        setProperty("fixtureRuntimeClasspath", System.getProperty("incremental.runtimeClasspath"))
        setProperty("fixtureCompilerPlugin", System.getProperty("incremental.compilerPlugin"))
      }
    File(projectDir, "gradle.properties").outputStream().use { properties.store(it, null) }
    File(projectDir, "build.gradle")
      .writeText(
        """
      plugins {
        id 'org.jetbrains.kotlin.jvm' version '${System.getProperty("incremental.kotlinVersion")}' apply false
        id 'dev.zacsweers.metro' version '${System.getProperty("incremental.metroVersion")}' apply false
      }
      subprojects {
        apply plugin: 'org.jetbrains.kotlin.jvm'
        apply plugin: 'java-library'
        apply plugin: 'dev.zacsweers.metro'
        dependencies {
          implementation files(rootProject.fixtureRuntimeClasspath.split(File.pathSeparator))
          kotlinCompilerPluginClasspath files(rootProject.fixtureCompilerPlugin)
        }
        kotlin { jvmToolchain(21) }
        metro {
          generateClassesInIr.set(true)
          generateContributionHintsInFir.set(true)
          compilerOptions.enable('omit-redundant-mirrors')
        }
        tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask).configureEach {
          compilerOptions.freeCompilerArgs.add('-Xcompiler-plugin-order=software.ralf.app.platform.metro.compiler>dev.zacsweers.metro.compiler')
        }
      }
      project(':lib') {
        if (findProject(':models') != null) {
          dependencies { api project(':models') }
        }
      }
      project(':app') {
        apply plugin: 'application'
        dependencies { implementation project(':lib') }
        application { mainClass = 'com.test.MainKt' }
      }
      """
          .trimIndent(),
      )
  }

  private fun baseline(expected: String) {
    assertResult(result = build(), expected = expected)
    val unchanged = build()
    assertResult(result = unchanged, expected = expected)
    for (module in listOf("models", "lib", "app").filter { File(projectDir, it).exists() }) {
      assertEquals(
        expected = TaskOutcome.UP_TO_DATE,
        actual = unchanged.task(":$module:compileKotlin")?.outcome,
        message = unchanged.output,
      )
    }
  }

  private fun rebuild(expected: String, changedModules: List<String>) {
    val result = build()
    assertResult(result = result, expected = expected)
    val reportUri =
      result.output
        .lineSequence()
        .single { it.startsWith("Kotlin build report is written to ") }
        .removePrefix("Kotlin build report is written to ")
    val report = File(URI(reportUri)).readText()
    for (module in changedModules) {
      assertEquals(
        expected = TaskOutcome.SUCCESS,
        actual = result.task(":$module:compileKotlin")?.outcome,
        message = result.output,
      )
      val compilation =
        report.split(Regex("(?m)(?=^Task ')")).single {
          it.startsWith("Task ':$module:compileKotlin'")
        }
      assertTrue("Incremental compilation completed" in compilation, compilation)
      assertFalse("Non-incremental compilation will be performed" in compilation, compilation)
      assertFalse("Falling back to non-incremental compilation" in compilation, compilation)
    }
  }

  private fun assertResult(result: BuildResult, expected: String) {
    assertContains(result.output.lineSequence().toList(), "RESULT:$expected")
  }

  private fun replaceSource(module: String, old: String, new: String) {
    val source = File(projectDir, "$module/src/main/kotlin/Contribution.kt")
    val contents = source.readText()
    assertTrue(old in contents, "Missing replacement in $source: $old")
    source.writeText(contents.replace(oldValue = old, newValue = new))
  }

  private fun build(): BuildResult =
    GradleRunner.create()
      .withProjectDir(projectDir)
      .withArguments(":app:run", "--no-build-cache", "--stacktrace", "--console=plain", "--info")
      .build()
}
