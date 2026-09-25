package software.ralf.app.platform.gradle.buildsrc

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
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

  @OptIn(ExperimentalAbiValidation::class)
  private fun Project.configureBinaryCompatibility() {
    extensions.getByType(KotlinBaseExtension::class.java).abiValidation {
      it.keepLocallyUnsupportedTargets.set(!ci)

      // These packages only contain generated hints consumed by compiler plugins.
      it.filters.exclude.byNames.addAll(
        "app.platform.inject.**",
        "amazon.lastmile.inject.**",
        "metro.hints.**",
      )
    }

    releaseTask.configure { it.dependsOn("checkKotlinAbi") }
  }

  private fun Project.configureExplicitApi() {
    extensions.getByType(KotlinBaseExtension::class.java).explicitApi()
  }

  private val Project.mavenPublish: MavenPublishBaseExtension
    get() = extensions.getByType(MavenPublishBaseExtension::class.java)
}
