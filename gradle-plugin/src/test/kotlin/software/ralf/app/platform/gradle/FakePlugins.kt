package software.ralf.app.platform.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

public class FakeAndroidApplicationPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeAndroidLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeAndroidKmpLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) = Unit
}

public class FakeKotlinAndroidPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeKotlinJvmPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.addSinglePlatformConfigurations()
  }
}

public class FakeMetroPlugin : Plugin<Project> {
  override fun apply(target: Project) = Unit
}

private fun Project.addSinglePlatformConfigurations() {
  listOf(
      "api",
      "implementation",
      "testImplementation",
      "androidTestImplementation",
      "ksp",
      PLUGIN_CLASSPATH_CONFIGURATION_NAME,
      "${PLUGIN_CLASSPATH_CONFIGURATION_NAME}Debug",
      NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME,
    )
    .forEach { configurationName -> configurations.maybeCreate(configurationName) }
}
