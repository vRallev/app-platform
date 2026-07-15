package software.ralf.app.platform.scope.di

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.internal.Platform
import software.ralf.app.platform.internal.platform
import software.ralf.app.platform.scope.Scope

class ComponentServiceTest {

  @Test
  fun `a DI component can be registered in a scope`() {
    val component = ParentComponentImpl()

    val scope = Scope.buildRootScope { addKotlinInjectComponent(component) }

    assertThat(scope.kotlinInjectComponent<ParentComponent>()).isSameInstanceAs(component)
  }

  @Test
  @IgnoreWasm
  fun `if a DI component cannot be found then an exception is thrown with a helpful error message`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addKotlinInjectComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addKotlinInjectComponent(childComponent) }

    val exception =
      assertFailsWith<NoSuchElementException> { childScope.kotlinInjectComponent<Unit>() }

    val kotlinReflectWarning =
      when (platform) {
        Platform.JVM -> " (Kotlin reflection is not available)"
        Platform.Native,
        Platform.Web -> ""
      }

    assertThat(exception)
      .hasMessage(
        "Couldn't find component implementing class kotlin.Unit$kotlinReflectWarning. " +
          "Inspected: [ChildComponentImpl, ParentComponentImpl] (fully qualified names: " +
          "[class software.ralf.app.platform.scope.di.ComponentServiceTest." +
          "ChildComponentImpl$kotlinReflectWarning, class software.ralf.app." +
          "platform.scope.di.ComponentServiceTest.ParentComponentImpl" +
          "$kotlinReflectWarning])"
      )
  }

  @Test
  fun `a DI component can be retrieved from a scope`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addKotlinInjectComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addKotlinInjectComponent(childComponent) }

    assertThat(childScope.kotlinInjectComponent<ChildComponent>()).isSameInstanceAs(childComponent)
    assertThat(childScope.kotlinInjectComponent<ParentComponent>())
      .isSameInstanceAs(parentComponent)

    assertThat(parentScope.kotlinInjectComponent<ParentComponent>())
      .isSameInstanceAs(parentComponent)
    assertFailsWith<NoSuchElementException> { parentScope.kotlinInjectComponent<ChildComponent>() }
  }

  private interface ParentComponent

  private class ParentComponentImpl : ParentComponent

  private interface ChildComponent

  private class ChildComponentImpl : ChildComponent
}
