package software.ralf.app.platform.robot

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isSameInstanceAs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import software.ralf.app.platform.renderer.metro.RobotKey
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph

class RobotGraphTest {

  @Test
  fun `application graphs expose an empty robot map without explicit wiring`() {
    val graph = createGraph<EmptyAppGraph>() as RobotGraph

    assertThat(graph.robots).isEmpty()
  }

  @Test
  fun `custom graphs can include the empty robot map binding`() {
    val graph = createGraph<EmptyCustomGraph>()

    assertThat(graph.robots).isEmpty()
  }

  @Test
  fun `robots from a custom child graph are found below an empty application graph`() {
    val rootScope = Scope.buildRootScope {
      addMetroDependencyGraph(createGraph<EmptyAppGraph>())
    }
    rootScope.buildChild("child") { addMetroDependencyGraph(createGraph<ChildGraph>()) }

    try {
      robot<TestRobot>(rootScope) { assertThat(this).isSameInstanceAs(TestRobot) }
    } finally {
      rootScope.destroy()
    }
  }

  @DependencyGraph(AppScope::class) interface EmptyAppGraph

  @DependencyGraph(bindingContainers = [RobotGraph.Bindings::class])
  interface EmptyCustomGraph : RobotGraph

  @DependencyGraph
  interface ChildGraph : RobotGraph {
    @Provides @IntoMap @RobotKey(TestRobot::class) fun provideRobot(): Robot = TestRobot
  }

  object TestRobot : Robot
}
