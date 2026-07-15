package software.ralf.app.platform.scope.di.metro

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.internal.Platform
import software.ralf.app.platform.internal.platform
import software.ralf.app.platform.scope.Scope

class MetroServiceTest {

  @Test
  fun `a metro graph can be registered in a scope`() {
    val graph = ParentGraphImpl()

    val scope = Scope.buildRootScope { addMetroDependencyGraph(graph) }

    assertThat(scope.metroDependencyGraph<ParentGraph>()).isSameInstanceAs(graph)
  }

  @Test
  @IgnoreWasm
  fun `if a metro graph cannot be found then an exception is thrown with a helpful error message`() {
    val parentGraph = ParentGraphImpl()
    val childGraph = ChildGraphImpl()

    val parentScope = Scope.buildRootScope { addMetroDependencyGraph(parentGraph) }
    val childScope = parentScope.buildChild("child") { addMetroDependencyGraph(childGraph) }

    val exception =
      assertFailsWith<NoSuchElementException> { childScope.metroDependencyGraph<Unit>() }

    val kotlinReflectWarning =
      when (platform) {
        Platform.JVM -> " (Kotlin reflection is not available)"
        Platform.Native,
        Platform.Web -> ""
      }

    assertThat(exception)
      .hasMessage(
        "Couldn't find dependency graph implementing class kotlin.Unit$kotlinReflectWarning. " +
          "Inspected: [ChildGraphImpl, ParentGraphImpl] (fully qualified names: " +
          "[class software.ralf.app.platform.scope.di.metro.MetroServiceTest." +
          "ChildGraphImpl$kotlinReflectWarning, class software.ralf.app." +
          "platform.scope.di.metro.MetroServiceTest.ParentGraphImpl" +
          "$kotlinReflectWarning])"
      )
  }

  @Test
  fun `a DI graph can be retrieved from a scope`() {
    val parentGraph = ParentGraphImpl()
    val childGraph = ChildGraphImpl()

    val parentScope = Scope.buildRootScope { addMetroDependencyGraph(parentGraph) }
    val childScope = parentScope.buildChild("child") { addMetroDependencyGraph(childGraph) }

    assertThat(childScope.metroDependencyGraph<ChildGraph>()).isSameInstanceAs(childGraph)
    assertThat(childScope.metroDependencyGraph<ParentGraph>()).isSameInstanceAs(parentGraph)

    assertThat(parentScope.metroDependencyGraph<ParentGraph>()).isSameInstanceAs(parentGraph)
    assertFailsWith<NoSuchElementException> { parentScope.metroDependencyGraph<ChildGraph>() }
  }

  private interface ParentGraph

  private class ParentGraphImpl : ParentGraph

  private interface ChildGraph

  private class ChildGraphImpl : ChildGraph
}
