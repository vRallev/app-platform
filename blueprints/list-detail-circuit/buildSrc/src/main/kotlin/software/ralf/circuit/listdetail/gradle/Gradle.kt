package software.ralf.circuit.listdetail.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.Packaging
import java.util.Locale
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
  get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal val Project.ci: Boolean
  get() = providers.environmentVariable("CI").isPresent

internal val Project.androidCompileSdk: Int
  get() = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()

internal val Project.androidMinSdk: Int
  get() = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

internal val Project.androidTargetSdk: Int
  get() = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()

internal val Project.javaVersion: JavaVersion
  get() = JavaVersion.toVersion(libs.findVersion("jvm-target").get().requiredVersion)

internal val Project.jvmTarget: JvmTarget
  get() = JvmTarget.fromTarget(javaVersion.toString())

internal val Project.kmpExtension: KotlinMultiplatformExtension
  get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

internal val Project.androidExtension: CommonExtension
  get() = extensions.getByType(CommonExtension::class.java)

internal val Project.androidKmpTarget: KotlinMultiplatformAndroidLibraryTarget
  get() =
    (kmpExtension as ExtensionAware)
      .extensions
      .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)

internal val Project.releaseTask: TaskProvider<Task>
  get() = tasks.named("release")

internal val Project.safePathString: String
  get() = path.removePrefix(":").replace(':', '-')

internal val Project.versionName: String
  get() = providers.gradleProperty("VERSION_NAME").get()

internal fun configureAndroidLint(lint: Lint) {
  lint.warningsAsErrors = true
  lint.disable +=
    setOf(
      "GradleDependency",
      "ObsoleteLintCustomCheck",
      "NewerVersionAvailable",
      "AndroidGradlePluginVersion",
      "OldTargetApi",
    )
}

internal fun configureAndroidPackaging(packaging: Packaging) {
  packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
