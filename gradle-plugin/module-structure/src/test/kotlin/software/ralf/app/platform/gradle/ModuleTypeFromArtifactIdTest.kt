package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ModuleTypeFromArtifactIdTest {
  @Test
  fun `artifact IDs follow the module naming conventions`() {
    mapOf(
        "feature-public" to ModuleType.PUBLIC,
        "feature-public-robots" to ModuleType.PUBLIC_ROBOTS,
        "feature-testing" to ModuleType.TESTING,
        "feature-impl" to ModuleType.IMPL,
        "feature-impl-debug" to ModuleType.IMPL,
        "feature-impl-robots" to ModuleType.IMPL_ROBOTS,
        "feature-impl-debug-robots" to ModuleType.IMPL_ROBOTS,
        "feature-internal" to ModuleType.INTERNAL,
        "feature-internal-debug" to ModuleType.INTERNAL,
        "feature-internal-robots" to ModuleType.INTERNAL_ROBOTS,
        "feature-internal-debug-robots" to ModuleType.INTERNAL_ROBOTS,
        "app" to ModuleType.APP,
        "app-desktop" to ModuleType.APP,
      )
      .forEach { (artifactId, expected) ->
        assertThat(artifactId.moduleTypeFromArtifactId(), artifactId).isEqualTo(expected)
      }
  }

  @Test
  fun `library suffixes take precedence over the app prefix`() {
    assertThat("app-public".moduleTypeFromArtifactId()).isEqualTo(ModuleType.PUBLIC)
    assertThat("app-testing".moduleTypeFromArtifactId()).isEqualTo(ModuleType.TESTING)
    assertThat("app-impl-debug-robots".moduleTypeFromArtifactId()).isEqualTo(ModuleType.IMPL_ROBOTS)
  }

  @Test
  fun `unrecognized artifact IDs have unknown module type`() {
    listOf("", "feature", "application", "feature-app", "feature-robots").forEach { artifactId ->
      assertThat(artifactId.moduleTypeFromArtifactId(), artifactId).isEqualTo(ModuleType.UNKNOWN)
    }
  }
}
