package software.ralf.app.platform.gradle

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import software.ralf.app.platform.gradle.ModuleType.APP
import software.ralf.app.platform.gradle.ModuleType.IMPL
import software.ralf.app.platform.gradle.ModuleType.INTERNAL
import software.ralf.app.platform.gradle.ModuleType.PUBLIC
import software.ralf.app.platform.gradle.ModuleType.TESTING
import software.ralf.app.platform.gradle.ModuleType.UNKNOWN

class ModuleTypeTest {

  @Test
  fun `project path is app for app and hyphenated app module names`() {
    listOf(
        ":app",
        ":app-mobile",
        ":apps",
        ":apps-mobile",
        ":feature:app-public",
      )
      .forEach { modulePath -> assertThat(modulePath.moduleTypeFromProjectPath()).isEqualTo(APP) }
  }

  @Test
  fun `project path is app when app segment is part of path`() {
    listOf(
        ":app:feature",
        ":apps:feature",
        ":app-mobile:feature",
        ":apps-mobile:feature",
      )
      .forEach { modulePath -> assertThat(modulePath.moduleTypeFromProjectPath()).isEqualTo(APP) }
  }

  @Test
  fun `project path keeps explicit module type when app segment is part of path`() {
    assertThat(":feature:app:public".moduleTypeFromProjectPath()).isEqualTo(PUBLIC)
    assertThat(":apps:abc:impl".moduleTypeFromProjectPath()).isEqualTo(IMPL)
    assertThat(":feature:app-mobile:testing".moduleTypeFromProjectPath()).isEqualTo(TESTING)
    assertThat(":feature:apps-mobile:internal".moduleTypeFromProjectPath()).isEqualTo(INTERNAL)
  }

  @Test
  fun `project path does not treat app lookalike parent segment as app`() {
    assertThat(":application:public".moduleTypeFromProjectPath()).isEqualTo(PUBLIC)
    assertThat(":feature:appstate".moduleTypeFromProjectPath()).isEqualTo(UNKNOWN)
    assertThat(":feature:appspecific:impl".moduleTypeFromProjectPath()).isEqualTo(IMPL)
    assertThat(":feature:appspecific:feature".moduleTypeFromProjectPath()).isEqualTo(UNKNOWN)
  }
}
