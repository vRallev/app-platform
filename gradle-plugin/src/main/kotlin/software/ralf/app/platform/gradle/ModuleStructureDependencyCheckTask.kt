package software.ralf.app.platform.gradle

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

  /** An empty output makes the task work with up-to-date checks. */
  @Suppress("unused") @get:OutputFile @get:Optional public abstract var ignoredOutputFile: File

  init {
    description = "Checks that our module structure dependency rules are followed."
    group = "Verification"
    allowLibraryImplToImplDependencies.convention(false)
  }

  @TaskAction
  @PublishedApi
  internal fun checkDependencies() {
    val moduleType = modulePath.moduleType

    if (moduleType == ModuleType.PUBLIC) {
      checkOnlyPublicModule()
    }
    if (moduleType != ModuleType.APP && moduleType != ModuleType.IMPL_ROBOTS) {
      checkNoImplImport(moduleType)
    }
    if (moduleType != ModuleType.TESTING && !moduleType.isRobotsModule) {
      checkNoTestingImport()
    }
    if (!moduleType.isRobotsModule) {
      checkNoRobotsImport()
    }
    if (moduleType != ModuleType.APP) {
      checkNoInternalImportFromOtherLibrary()
    }
  }

  private fun checkOnlyPublicModule() {
    val forbiddenDependencies = moduleCompileClasspath.filter { it.moduleType != ModuleType.PUBLIC }

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
    public fun Project.registerModuleStructureDependencyCheckTask() {
      val baseTaskName = "checkModuleStructureDependencies"
      val baseTask =
        tasks.register(baseTaskName) {
          it.description = "Checks that our module structure dependency rules for all targets."
          it.group = "Verification"
        }

      afterEvaluate { tasks.namedOptional("check") { it.dependsOn(baseTask) } }

      fun registerForConfiguration(taskSuffix: String, configuration: () -> Configuration) {
        val checkTask =
          tasks.register(
            "$baseTaskName${taskSuffix.capitalize()}",
            ModuleStructureDependencyCheckTask::class.java,
          ) { task ->
            task.modulePath = path
            task.allowLibraryImplToImplDependencies.set(
              appPlatform.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed()
            )
            task.moduleCompileClasspath =
              configuration()
                .allDependencies
                .mapNotNull { dependency ->
                  when (dependency) {
                    is ExternalDependency -> {
                      "${dependency.group}:${dependency.name}:${dependency.version}"
                        .takeIf { dependency.name.moduleTypeFromArtifactId() != ModuleType.UNKNOWN }
                    }

                    is ProjectDependency -> {
                      dependency.path.takeIf {
                        it.moduleTypeFromProjectPath() != ModuleType.UNKNOWN
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
        }
      }

      plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
        kmpExtension.targets.configureEach { target ->
          // We register Android above.
          if (target.name == "android") return@configureEach

          target.compilations.configureEach configureEach2@{ compilation ->
            // We only care about main.
            if (compilation.name != "main") return@configureEach2

            registerForConfiguration(
              taskSuffix = target.name,
              configuration = {
                configurations.getByName(compilation.compileDependencyConfigurationName)
              },
            )
          }
        }
      }

      plugins.withId(PluginIds.KOTLIN_JVM) {
        registerForConfiguration(
          taskSuffix = "jvm",
          configuration = { configurations.getByName("compileClasspath") },
        )
      }
    }
  }
}
