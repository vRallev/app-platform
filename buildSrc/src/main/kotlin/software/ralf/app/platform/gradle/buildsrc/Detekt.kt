package software.ralf.app.platform.gradle.buildsrc

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask

internal fun Project.configureDetekt() {
  plugins.apply(Plugins.DETEKT)

  fun SourceTask.configureDefaultDetektTask() {
    // The :detekt task in a multiplatform project doesn't do anything, it has no sources
    // configured. Instead, the Detekt plugin creates a Gradle task for each source set, which then
    // need to be called manually. Configure the default task once so every project follows the same
    // faster path.
    setSource(layout.files("src"))
    exclude("**/*.kts")
    exclude("**/api/**")
    exclude("**/build/**")
    exclude("**/detekt/**")
  }

  tasks.withType(Detekt::class.java).configureEach { detekt ->
    detekt.jvmTarget.set(javaVersion.toString())

    if (detekt.name == "detekt") {
      detekt.configureDefaultDetektTask()
    }
  }
  tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
    it.jvmTarget.set(javaVersion.toString())

    if (it.name == "detektBaseline") {
      it.configureDefaultDetektTask()
    }
  }
  with(extensions.getByType(DetektExtension::class.java)) {
    // From the Groovy DSL at https://detekt.github.io/detekt/gradle.html#groovy-dsl-3
    // This produces baselines named "detekt-baseline.xml"
    baseline.set(file("detekt/detekt-baseline.xml"))
    // Config overrides
    config.from(rootDir.resolve("gradle/detekt-config.yml"))
    buildUponDefaultConfig.set(true)
  }

  releaseTask.configure { releaseTask -> releaseTask.dependsOn("detekt") }
}
