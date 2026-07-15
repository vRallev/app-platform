package software.ralf.app.platform.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import software.ralf.app.platform.gradle.ModuleStructureDependencyCheckTask.Companion.registerModuleStructureDependencyCheckTask

/** The Gradle plugin that sets up our module structure. */
public open class ModuleStructurePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.ensureFollowsNamingConvention()
    target.addModuleStructureDependencies()
    target.configureAndroidNamespace()
    target.registerModuleStructureDependencyCheckTask()
  }

  private fun Project.ensureFollowsNamingConvention() {
    check(isUsingModuleStructure()) {
      "$path enables the module structure, but the project name doesn't follow the naming convention."
    }
  }

  private fun Project.addModuleStructureDependencies() {
    fun addModuleStructureDependencies(
      publicModuleConfiguration: String,
      implModuleConfiguration: String,
    ) {
      val parent = requireParent()

      // Nothing to add.
      if (isPublicModule()) return

      fun addPublicModule() {
        // this is ok because no properties within publicModule are accessed
        @Suppress("GradleProjectIsolation") val publicModule = findProject("${parent.path}:public")
        if (publicModule != null) {
          dependencies.add(publicModuleConfiguration, publicModule)
        }
      }

      when {
        isTestingModule() -> {
          // :testing modules provide helper functions or fake implementations of the
          // APIs in the :public module.
          addPublicModule()
        }

        isImplModule() || isInternalModule() -> {
          // :impl and :internal modules implement interfaces and types from the :public
          // module.
          addPublicModule()
        }

        isRobotsModule() -> {
          // :robot modules usually reference types from the :public and :impl modules.
          addPublicModule()

          // Add a dependency to the implementation module. Note that an "implementation"
          // dependency is chosen rather than an "api" dependency. The goal of the a
          // robots module to hide all details of the :impl module and only expose
          // abstractions with the help of robots.
          @Suppress("GradleProjectIsolation") // no properties within project are accessed
          findProject(path.substringBefore("-robots"))
            ?.takeIf { it.isImplModule() }
            ?.let { implModule -> dependencies.add(implModuleConfiguration, implModule) }
        }
      }
    }

    plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
      addModuleStructureDependencies(
        publicModuleConfiguration = "commonMainApi",
        implModuleConfiguration = "commonMainImplementation",
      )
    }

    withJvmOrAndroidPlugin {
      addModuleStructureDependencies(
        publicModuleConfiguration = "api",
        implModuleConfiguration = "implementation",
      )
    }
  }

  private fun Project.configureAndroidNamespace() {
    plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
      // Do not override any configured namespace.
      if (android.namespace == null) {
        android.namespace = namespace()
      }
    }
    plugins.withId(PluginIds.ANDROID_KMP_LIBRARY) {
      if (androidKmpTarget.namespace == null) {
        androidKmpTarget.namespace = namespace()
      }
    }
  }

  public companion object {

    /**
     * Returns a consistent namespace for a Gradle module that has the recommended App Platform
     * module structure in mind. It helps to avoid clashing namespaces across projects.
     *
     * This value can be used as namespace for Android projects and gets automatically set when no
     * other namespace is declared.
     *
     * It requires that the `GROUP` property is set for the Gradle project.
     *
     * E.g. it produces following results:
     * ```
     * GROUP=software.ralf.abc
     *
     * :def:public  -> "software.ralf.abc.def"
     * :def:impl  -> "software.ralf.abc.def.impl"
     * :def:impl-ghj-robots  -> "software.ralf.abc.def.impl.ghj.robots"
     * ```
     *
     * @see com.android.build.api.dsl.CommonExtension.namespace
     */
    public fun Project.namespace(): String {
      val group =
        providers.gradleProperty("GROUP").let {
          check(it.isPresent) {
            "Couldn't find the GROUP property for this project. Make sure you define " +
              "a group in the project's gradle.properties file, e.g. `GROUP=" +
              "software.ralf.abc`."
          }
          return@let it.get()
        }

      val path =
        when {
          isPublicModule() -> requireParent().path
          isAnyPublicModule() && isRobotsModule() -> "${requireParent().path}:robots"
          else -> path
        }

      return "$group${path.replace(':', '.').replace('-', '.')}"
    }

    /**
     * Returns a consistent artifact ID for a Gradle module that has the recommended App Platform
     * module structure in mind. This artifact ID should be used for publishing library modules.
     *
     * It produces following results:
     * ```
     * :abc:public  -> "abc-public"
     * :abc:impl-def-robots  -> "abc-impl-def-robots"
     * ```
     */
    public fun Project.artifactId(libraryName: String = requireParent().name): String {
      return "$libraryName-$name"
    }

    internal val Project.testingSourceSets: List<String>
      get() = buildList {
        when {
          plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM) -> {
            add("commonTest")
            if (moduleType.useTestDependenciesInMain) {
              add("commonMain")
            }
          }

          plugins.hasPlugin(PluginIds.KOTLIN_ANDROID) ||
            plugins.hasPlugin(PluginIds.KOTLIN_JVM) ||
            plugins.hasPlugin(PluginIds.ANDROID_APP) ||
            plugins.hasPlugin(PluginIds.ANDROID_LIBRARY) -> {
            add("testImplementation")
            if (moduleType.useTestDependenciesInMain) {
              add("implementation")
            }
          }
        }
      }
  }
}
