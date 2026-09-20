package software.ralf.app.platform.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.project.IsolatedProject
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun PluginContainer.withIds(vararg pluginIds: String, action: (Plugin<*>) -> Unit) {
  pluginIds.forEach { id -> withId(id) { action(it) } }
}

/**
 * Runs [action] once for single-platform JVM and Android projects. Android projects may use either
 * the legacy Kotlin Android plugin or AGP built-in Kotlin.
 */
internal fun Project.withJvmOrAndroidPlugin(action: () -> Unit) {
  var didRun = false

  fun runOnce() {
    if (didRun || plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM)) return

    didRun = true
    action()
  }

  plugins.withIds(
    PluginIds.KOTLIN_ANDROID,
    PluginIds.KOTLIN_JVM,
    PluginIds.ANDROID_APP,
    PluginIds.ANDROID_LIBRARY,
  ) {
    runOnce()
  }
}

/** Runs [action] once for projects using the legacy Kotlin Android plugin or Kotlin JVM. */
internal fun Project.withLegacyKotlinJvmOrAndroidPlugin(action: () -> Unit) {
  var didRun = false

  fun runOnce() {
    if (didRun || plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM)) return

    didRun = true
    action()
  }

  plugins.withIds(PluginIds.KOTLIN_ANDROID, PluginIds.KOTLIN_JVM) {
    runOnce()
  }
}

/** Runs [action] once for Android projects using AGP built-in Kotlin. */
internal fun Project.withAgpBuiltInKotlinAndroidPlugin(action: () -> Unit) {
  var didRun = false

  fun runOnce() {
    if (
      didRun ||
        plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM) ||
        plugins.hasPlugin(PluginIds.KOTLIN_ANDROID)
    ) {
      return
    }

    didRun = true
    action()
  }

  plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
    runOnce()
  }
}

// This is OK because no properties within parent are accessed
// https://github.com/gradle/gradle/issues/33198
@Suppress("GradleProjectIsolation")
internal fun Project.requireParent(): IsolatedProject =
  requireNotNull(parent) {
      "The parent project for a module enabling the module structure should not be null."
    }
    .isolated

internal val Project.android: CommonExtension
  get() = extensions.getByType(CommonExtension::class.java)

internal val Project.androidComponents: AndroidComponentsExtension<*, *, *>
  get() = extensions.getByType(AndroidComponentsExtension::class.java)

internal val Project.kmpExtension: KotlinMultiplatformExtension
  get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

internal val Project.androidKmpTarget: KotlinMultiplatformAndroidLibraryTarget
  get() =
    (kmpExtension as ExtensionAware)
      .extensions
      .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)

internal fun TaskContainer.namedOptional(name: String, configurationAction: (Task) -> Unit) {
  try {
    named(name, configurationAction)
  } catch (_: UnknownTaskException) {}
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
