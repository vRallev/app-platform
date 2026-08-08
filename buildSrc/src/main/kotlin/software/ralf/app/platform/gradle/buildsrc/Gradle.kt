package software.ralf.app.platform.gradle.buildsrc

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import java.util.Locale
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import software.ralf.app.platform.gradle.moduleType

internal val Project.libs: VersionCatalog
  get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal val Project.ci
  get() = providers.gradleProperty("CI").isPresent || System.getenv("CI") != null

internal val Project.javaVersion
  get() = JavaVersion.toVersion(libs.findVersion("jvm.compatibility").get().requiredVersion)
internal val Project.javaTarget
  get() = JvmTarget.fromTarget(javaVersion.toString())

internal val Project.safePathString: String
  get() = path.replace(':', '-').substring(1)

internal val Project.isKmpModule: Boolean
  get() = plugins.hasPlugin(Plugins.KOTLIN_MULTIPLATFORM)
internal val Project.isRoot: Boolean
  get() = path == ":"

internal val Project.android: CommonExtension
  get() = extensions.getByType(CommonExtension::class.java)

internal val Project.androidComponents: AndroidComponentsExtension<*, *, *>
  get() = extensions.getByType(AndroidComponentsExtension::class.java)

internal val Project.androidKmpTarget: KotlinMultiplatformAndroidLibraryTarget
  get() =
    (extensions.getByType(KotlinMultiplatformExtension::class.java) as ExtensionAware)
      .extensions
      .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)

internal val Project.releaseTask: TaskProvider<Task>
  get() = tasks.named("release")

internal val Project.versionName: String
  get() = requireNotNull(property("VERSION_NAME")).toString()

internal fun Project.useTestDependenciesInMain(): Boolean {
  return moduleType.useTestDependenciesInMain || path.startsWith(":robot")
}

internal fun PluginContainer.withIds(vararg pluginIds: String, action: (Plugin<*>) -> Unit) {
  pluginIds.forEach { id -> withId(id) { action(it) } }
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
