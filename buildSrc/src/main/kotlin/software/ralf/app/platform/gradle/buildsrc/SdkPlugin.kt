package software.ralf.app.platform.gradle.buildsrc

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import software.ralf.app.platform.gradle.ModuleStructurePlugin.Companion.artifactId

internal object SdkPlugin {
  fun Project.publishSdk() {
    mavenPublishing()
    configureBinaryCompatibility()
    configureExplicitApi()
  }

  private fun Project.mavenPublishing() {
    // This plugin will add Gradle tasks to generate a source and javadoc .jar files, to
    // generate the .pom file and to publish the binaries in the local maven repository and
    // other repositories when needed.
    plugins.apply(Plugins.MAVEN_PUBLISH)

    // :presenter:public  -> ${group}:presenter-public:${version}
    // :presenter:impl    -> ${group}:presenter-impl:${version}
    // :presenter:testing -> ${group}:presenter-testing:${version}
    val parent = requireNotNull(parent)
    val artifactId =
      when {
        parent.name == "contribute" && parent.parent?.name == "kotlin-inject-extensions" -> {
          // Change the artifact ID, because "contribute" alone is a weird name.
          artifactId(libraryName = "kotlin-inject-contribute")
        }
        parent.name == "contribute" && parent.parent?.name == "metro-extensions" -> {
          // Change the artifact ID, because "contribute" alone is a weird name.
          artifactId(libraryName = "metro-contribute")
        }
        else -> {
          artifactId()
        }
      }
    mavenPublish.coordinates(artifactId = artifactId)
    mavenPublish.pom { pom ->
      pom.name.set(
        "App Platform ${
        artifactId.split('-')
          .joinToString(separator = " ", prefix = "", postfix = "") { it.capitalize() }
      }"
      )
    }
  }

  private fun Project.configureBinaryCompatibility() {
    // This plugin ensures that binary changes are committed as a human readable text file
    // in the repository.
    plugins.apply(Plugins.BINARY_COMPAT_VALIDATOR)

    releaseTask.configure { it.dependsOn("apiCheck") }

    val apiValidation = extensions.getByType(ApiValidationExtension::class.java)

    // Klib doesn't work in CI right now and this creates mismatch between local and CI builds.
    // Disable the experimental feature for now.
    @Suppress("OPT_IN_USAGE")
    apiValidation.klib.enabled = false

    // These packages only contain generated code that is picked up by compiler plugins.
    // They don't need to be part of the API dumps.
    apiValidation.ignoredPackages +=
      setOf("app.platform.inject", "amazon.lastmile.inject", "metro.hints")
  }

  private fun Project.configureExplicitApi() {
    extensions.getByType(KotlinBaseExtension::class.java).explicitApi()
  }

  private val Project.mavenPublish: MavenPublishBaseExtension
    get() = extensions.getByType(MavenPublishBaseExtension::class.java)
}
