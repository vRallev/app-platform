package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import groovy.lang.Binding
import groovy.lang.GroovyShell
import kotlin.test.Test
import org.gradle.api.Action
import org.gradle.testfixtures.ProjectBuilder

class AppPlatformExtensionTest {

  @Test
  fun `module structure options are disabled by default`() {
    val extension = createExtension()

    assertThat(extension.isModuleStructureEnabled().get()).isFalse()
    assertThat(extension.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed().get())
      .isFalse()
  }

  @Test
  fun `module structure can be enabled and configured with an action`() {
    val extension = createExtension()

    extension.enableModuleStructure(
      Action { options -> options.allowLibraryImplToImplDependencies(true) }
    )

    assertThat(extension.isModuleStructureEnabled().get()).isTrue()
    assertThat(extension.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed().get())
      .isTrue()
  }

  @Test
  fun `module structure options can be configured after it was already enabled`() {
    val extension = createExtension()
    extension.enableModuleStructure(true)

    extension.enableModuleStructure(
      Action { options -> options.allowLibraryImplToImplDependencies(true) }
    )

    assertThat(extension.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed().get())
      .isTrue()
  }

  @Test
  fun `module structure can be enabled and configured with the Groovy DSL`() {
    val extension = createExtension()
    val binding = Binding(mapOf("appPlatform" to extension))

    GroovyShell(binding)
      .evaluate(
        """
        appPlatform.enableModuleStructure {
          allowLibraryImplToImplDependencies true
        }
        """
          .trimIndent()
      )

    assertThat(extension.isModuleStructureEnabled().get()).isTrue()
    assertThat(extension.moduleStructureOptions().isLibraryImplToImplDependenciesAllowed().get())
      .isTrue()
  }

  private fun createExtension(): AppPlatformExtension {
    val rootProject = ProjectBuilder.builder().withName("root").build()
    val moduleProject = ProjectBuilder.builder().withName("impl").withParent(rootProject).build()
    moduleProject.plugins.apply(AppPlatformPlugin::class.java)
    return moduleProject.extensions.getByType(AppPlatformExtension::class.java)
  }
}
