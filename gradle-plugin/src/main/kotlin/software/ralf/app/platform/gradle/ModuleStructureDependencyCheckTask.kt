package software.ralf.app.platform.gradle

import com.android.build.api.variant.HasTestFixtures
import com.android.build.api.variant.TestComponent
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import software.ralf.app.platform.gradle.AppPlatformExtension.Companion.appPlatform

/** Checks that our module structure dependency rules are followed. */
@CacheableTask
public abstract class ModuleStructureDependencyCheckTask : DefaultTask() {

  /** The path of this module, e.g. `:presenter:public`. */
  @get:Input public abstract var modulePath: String

  /** All Gradle modules on the compile classpath. */
  @get:Input public abstract var moduleCompileClasspath: Set<String>

  /** Whether `:impl` dependencies within the same library are allowed. */
  @get:Input public abstract val allowLibraryImplToImplDependencies: Property<Boolean>

  /** Whether the checked compile classpath belongs to test fixtures. */
  @get:Input public abstract val testFixtures: Property<Boolean>

  /** Whether the checked compile classpath belongs to a test compilation. */
  @get:Input public abstract val testCompilation: Property<Boolean>

  /** An empty output makes the task work with up-to-date checks. */
  @Suppress("unused") @get:OutputFile @get:Optional public abstract var ignoredOutputFile: File

  init {
    description = "Checks that our module structure dependency rules are followed."
    group = "Verification"
    allowLibraryImplToImplDependencies.convention(false)
    testFixtures.convention(false)
    testCompilation.convention(false)
  }

  @TaskAction
  @PublishedApi
  internal fun checkDependencies() {
    val moduleType = modulePath.moduleType
    val testOnlyClasspath = testFixtures.get() || testCompilation.get()

    if (moduleType == ModuleType.PUBLIC) {
      checkOnlyPublicModule()
    }
    if (moduleType != ModuleType.APP && moduleType != ModuleType.IMPL_ROBOTS) {
      checkNoImplImport(moduleType)
    }
    if (moduleType != ModuleType.TESTING && !moduleType.isRobotsModule && !testOnlyClasspath) {
      checkNoTestingImport()
    }
    if (!moduleType.isRobotsModule && !testCompilation.get()) {
      checkNoRobotsImport()
    }
    if (moduleType != ModuleType.APP) {
      checkNoInternalImportFromOtherLibrary()
    }
  }

  private fun checkOnlyPublicModule() {
    val forbiddenDependencies = moduleCompileClasspath.filter {
      it.moduleType != ModuleType.PUBLIC &&
        !((testFixtures.get() || testCompilation.get()) && it.moduleType == ModuleType.TESTING) &&
        !(testCompilation.get() && it.moduleType.isRobotsModule)
    }

    if (forbiddenDependencies.isNotEmpty()) {
      throw GradleException(
        ":public modules are only allowed to depend on other :public modules. " +
          "Remove the dependencies: ${forbiddenDependencies.joinToString()} " +
          "from $modulePath."
      )
    }
  }

  private fun checkNoImplImport(moduleType: ModuleType) {
    val forbiddenDependencies =
      moduleCompileClasspath
        .filter { it.moduleType == ModuleType.IMPL }
        .filterNot { dependency ->
          allowLibraryImplToImplDependencies.get() &&
            moduleType == ModuleType.IMPL &&
            dependency.isProjectDependencyWithinSameLibrary()
        }

    if (forbiddenDependencies.isNotEmpty()) {
      throw GradleException(
        "No module except for an app module is allowed to import an :impl module. " +
          "Remove the dependencies: ${forbiddenDependencies.joinToString()} " +
          "from $modulePath."
      )
    }
  }

  private fun checkNoTestingImport() {
    val forbiddenDependencies = moduleCompileClasspath.filter {
      it.moduleType == ModuleType.TESTING
    }

    if (forbiddenDependencies.isNotEmpty()) {
      throw GradleException(
        "Testing modules should be added to the test compile classpath, otherwise " +
          "they're included in the final app. Remove the dependencies: " +
          "${forbiddenDependencies.joinToString()} from $modulePath."
      )
    }
  }

  private fun checkNoRobotsImport() {
    val forbiddenDependencies = moduleCompileClasspath.filter { it.moduleType.isRobotsModule }

    if (forbiddenDependencies.isNotEmpty()) {
      throw GradleException(
        "Robot modules should be added to the instrumented test compile classpath, " +
          "otherwise they're included in the final app. Remove the dependencies: " +
          "${forbiddenDependencies.joinToString()} from $modulePath."
      )
    }
  }

  private fun checkNoInternalImportFromOtherLibrary() {
    val forbiddenDependencies =
      moduleCompileClasspath
        .filter { it.moduleType == ModuleType.INTERNAL }
        .filter { dependency ->
          // Usually :internal modules are part of the same Gradle project, therefore the
          // dependency string starts with a colon ":", e.g. :library:internal. If that's
          // the case, then compare the parent path with this project's parent path. If
          // they match, then the :internal dependency is allowed. If they don't match,
          // then the dependency is forbidden.
          //
          // For external dependencies this check is much harder and for now we simply
          // assume that the internal dependency isn't allowed.
          !dependency.isProjectDependencyWithinSameLibrary()
        }

    if (forbiddenDependencies.isNotEmpty()) {
      throw GradleException(
        "Internal modules can only be imported within the same library or by app " +
          "modules, but not from another library. Remove the dependencies: " +
          "${forbiddenDependencies.joinToString()} from $modulePath."
      )
    }
  }

  private val String.moduleType: ModuleType
    get() =
      if (startsWith(':')) {
        moduleTypeFromProjectPath()
      } else {
        substringAfter(':').substringBefore(':').moduleTypeFromArtifactId()
      }

  private fun String.isProjectDependencyWithinSameLibrary(): Boolean =
    startsWith(":") && substringBeforeLast(':') == modulePath.substringBeforeLast(':')

  public companion object {
    /** Registers the task in the given project. */
    @Suppress("LongMethod")
    public fun Project.registerModuleStructureDependencyCheckTask() {
      val baseTaskName = "checkModuleStructureDependencies"
      val baseTask =
        tasks.register(baseTaskName) {
          it.description = "Checks that our module structure dependency rules for all targets."
          it.group = "Verification"
        }
      val dependencyChecksEnabled = appPlatform.moduleStructureOptions().isDependencyCheckEnabled()
      val testDependencyChecksEnabled =
        appPlatform.moduleStructureOptions().isTestDependencyCheckEnabled()

      plugins.withType(LifecycleBasePlugin::class.java).configureEach {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
          it.dependsOn(baseTask)
        }
      }

      fun registerForConfiguration(
        taskSuffix: String,
        configuration: () -> Configuration,
        isTestFixtures: Boolean = false,
        isTestCompilation: Boolean = false,
      ) {
        val checkTask =
          tasks.register(
            "$baseTaskName${taskSuffix.capitalize()}",
            ModuleStructureDependencyCheckTask::class.java,
          ) { task ->
            task.onlyIfDependencyChecksAreEnabled(
              dependencyChecksEnabled = dependencyChecksEnabled,
              testDependencyChecksEnabled = testDependencyChecksEnabled,
              isTestCompilation = isTestCompilation,
            )
            task.modulePath = path
            task.allowLibraryImplToImplDependencies.set(
              appPlatform.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed()
            )
            task.testFixtures.set(isTestFixtures)
            task.testCompilation.set(isTestCompilation)
            task.moduleCompileClasspath =
              configuration()
                .allDependencies
                .mapNotNull { dependency ->
                  when (dependency) {
                    is ExternalDependency -> dependency.moduleNotation(isTestCompilation)

                    is ProjectDependency -> {
                      dependency.path.takeIf {
                        it != path && it.moduleTypeFromProjectPath() != ModuleType.UNKNOWN
                      }
                    }

                    else -> null
                  }
                }
                .toSet()
          }

        baseTask.configure { it.dependsOn(checkTask) }
      }

      plugins.withIds(PluginIds.ANDROID_LIBRARY, PluginIds.ANDROID_APP) {
        androidComponents.onVariants { variant ->
          registerForConfiguration(
            taskSuffix = "android${variant.name.capitalize()}",
            configuration = { variant.compileConfiguration },
          )

          variant.nestedComponents.filterIsInstance<TestComponent>().forEach { testComponent ->
            registerForConfiguration(
              taskSuffix = "android${testComponent.name.capitalize()}",
              configuration = { testComponent.compileConfiguration },
              isTestCompilation = true,
            )
          }

          val testFixtures = (variant as? HasTestFixtures)?.testFixtures
          if (testFixtures != null) {
            registerForConfiguration(
              taskSuffix = "android${variant.name.capitalize()}TestFixtures",
              configuration = { testFixtures.compileConfiguration },
              isTestFixtures = true,
            )
          }
        }
      }

      plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
        fun KotlinTarget.registerCompilations() {
          compilations.configureEach { compilation ->
            // KGP has no public test-compilation marker. This repository uses *Test names.
            val isTestCompilation = compilation.name == "test" || compilation.name.endsWith("Test")
            if (compilation.name != "main" && !isTestCompilation) return@configureEach

            registerForConfiguration(
              taskSuffix =
                if (compilation.name == "main") {
                  name
                } else {
                  "$name${compilation.name.capitalize()}"
                },
              configuration = {
                configurations.getByName(compilation.compileDependencyConfigurationName)
              },
              isTestCompilation = isTestCompilation,
            )
          }
        }

        kmpExtension.targets.configureEach { target ->
          if (target.name == "android") {
            // Legacy Android variants are registered above. The Android-KMP plugin does not expose
            // those variants, so register its KMP compilations instead.
            plugins.withId(PluginIds.ANDROID_KMP_LIBRARY) {
              target.registerCompilations()
            }
            return@configureEach
          }

          target.registerCompilations()
        }
      }

      plugins.withId(PluginIds.KOTLIN_JVM) {
        registerForConfiguration(
          taskSuffix = "jvm",
          configuration = { configurations.getByName("compileClasspath") },
        )

        registerForConfiguration(
          taskSuffix = "jvmTest",
          configuration = { configurations.getByName("testCompileClasspath") },
          isTestCompilation = true,
        )

        plugins.withId("java-test-fixtures") {
          registerForConfiguration(
            taskSuffix = "jvmTestFixtures",
            configuration = { configurations.getByName("testFixturesCompileClasspath") },
            isTestFixtures = true,
          )
        }
      }
    }

    private fun ModuleStructureDependencyCheckTask.onlyIfDependencyChecksAreEnabled(
      dependencyChecksEnabled: Property<Boolean>,
      testDependencyChecksEnabled: Property<Boolean>,
      isTestCompilation: Boolean,
    ) {
      onlyIf("Module structure dependency checks are enabled") { dependencyChecksEnabled.get() }
      if (isTestCompilation) {
        onlyIf("Module structure test dependency checks are enabled") {
          testDependencyChecksEnabled.get()
        }
      }
    }

    private fun ExternalDependency.moduleNotation(testCompilation: Boolean): String? {
      val moduleType = name.moduleTypeFromArtifactId()
      return when {
        moduleType == ModuleType.UNKNOWN -> null
        // This test tool contains "internal" as an unrelated part of its artifact name.
        testCompilation &&
          group == "org.jetbrains.kotlin" &&
          name == "kotlin-compiler-internal-test-framework" -> null
        else -> "$group:$name:$version"
      }
    }
  }
}
