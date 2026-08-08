package software.ralf.app.platform.gradle.buildsrc

import com.android.build.api.dsl.Lint

internal fun Lint.configureAppPlatformLint() {
  warningsAsErrors = true
  disable +=
    setOf(
      "GradleDependency",
      "ObsoleteLintCustomCheck",
      "NewerVersionAvailable",
      "AndroidGradlePluginVersion",
      "OldTargetApi",
    )
}
