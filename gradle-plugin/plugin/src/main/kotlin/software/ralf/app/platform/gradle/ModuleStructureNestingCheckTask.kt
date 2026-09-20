package software.ralf.app.platform.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Checks project paths without producing outputs")
internal abstract class ModuleStructureNestingCheckTask : DefaultTask() {
  @get:Input abstract val projectPaths: SetProperty<String>

  @get:Input abstract val enableLibraryNestingCheck: Property<Boolean>

  @get:Input abstract val allowedNestedLibraryPaths: SetProperty<String>

  init {
    description = "Checks that library modules are direct children of their library."
    group = "Verification"
    enableLibraryNestingCheck.convention(true)
    allowedNestedLibraryPaths.convention(emptySet())
  }

  @TaskAction
  fun checkLibraryNesting() {
    if (!enableLibraryNestingCheck.get()) return

    val paths = projectPaths.get()
    val libraries =
      paths
        .filter { it.moduleTypeFromProjectPath() !in setOf(ModuleType.APP, ModuleType.UNKNOWN) }
        .map { it.substringBeforeLast(':') }
        .filter { it.isNotEmpty() }
        .toSortedSet()
    val exceptions = allowedNestedLibraryPaths.get()
    val invalidExceptions = (exceptions - libraries).sorted()
    if (invalidExceptions.isNotEmpty()) {
      throw GradleException(
        "allowNestedLibrariesIn requires exact library paths. These paths do not identify " +
          "libraries in the checked subtree: ${invalidExceptions.joinToString()}."
      )
    }

    val violations =
      libraries
        .filterNot { it in exceptions }
        .mapNotNull { library ->
          val nestedProjects =
            paths
              .filter { it.startsWith("$library:") && it.substringBeforeLast(':') != library }
              .sorted()
          if (nestedProjects.isEmpty()) null
          else "$library contains ${nestedProjects.joinToString()}"
        }
    if (violations.isNotEmpty()) {
      throw GradleException(
        "Library modules must be direct children of their library:\n" +
          violations.joinToString("\n") { " - $it" } +
          "\nMove nested projects outside the library, or configure allowNestedLibrariesIn " +
          "in the appPlatform.enableModuleStructureNestingCheck block of the project running this check."
      )
    }
  }

  companion object {
    fun Project.registerModuleStructureNestingCheckTask(
      options: ModuleStructureNestingCheckOptions
    ) {
      val checkTask =
        tasks.register(
          "checkModuleStructureNesting",
          ModuleStructureNestingCheckTask::class.java,
        ) { task ->
          task.projectPaths.set(subprojects.map { it.isolated.path })
          task.enableLibraryNestingCheck.set(options.isLibraryNestingCheckEnabled())
          task.allowedNestedLibraryPaths.set(options.allowedNestedLibraryPaths())
        }
      tasks
        .named { it == LifecycleBasePlugin.CHECK_TASK_NAME }
        .configureEach {
          it.dependsOn(checkTask)
        }
    }
  }
}
