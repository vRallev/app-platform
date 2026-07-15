package software.ralf.app.platform.robot

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isTrue
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.addKotlinInjectComponent
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph

class RobotTest {

  @Test
  fun `if no robot can be found in the component then a proper error is thrown`() {
    val exception =
      assertFailsWith<IllegalStateException> {
        robot<KiTestRobot>(rootScope(kiRobot = null, metroRobot = null)) {}
      }

    val message =
      exception.message?.replace("RobotTest\$KiTestRobot", "RobotTest.KiTestRobot").toString()

    assertThat(message)
      .contains(
        "Could not find Robot of type class software.ralf.app.platform." +
          "robot.RobotTest.KiTestRobot"
      )
    assertThat(message).contains("Did you forget to add the @ContributesRobot annotation?")
  }

  @Test
  fun `the close function is called after the lambda is invoked`() {
    val rootScope = rootScope(KiTestRobot())

    lateinit var robot: KiTestRobot
    robot<KiTestRobot>(rootScope) {
      robot = this
      assertThat(closeCalled).isFalse()
    }

    assertThat(robot.closeCalled).isTrue()
  }

  @Test
  fun `a new robot is instantiated every time the robot function is invoked`() {
    val rootScope = Scope.buildRootScope {
      addKotlinInjectComponent(
        object : RobotComponent {
          override val robots: Map<KClass<out Robot>, () -> Robot> =
            mapOf(KiTestRobot::class to { KiTestRobot() })
        }
      )
    }

    lateinit var robot1: KiTestRobot
    lateinit var robot2: KiTestRobot

    robot<KiTestRobot>(rootScope) { robot1 = this }
    robot<KiTestRobot>(rootScope) { robot2 = this }

    assertThat(robot1).isNotSameInstanceAs(robot2)

    robot<KiTestRobot>(rootScope) {
      val robot1Inner = this
      robot<KiTestRobot>(rootScope) {
        val robot2Inner = this
        assertThat(robot1Inner).isNotSameInstanceAs(robot2Inner)
      }
    }
  }

  @Test
  fun `a robot is provided for kotlin-inject alone`() {
    val rootScope = rootScope(kiRobot = KiTestRobot(), metroRobot = null)

    var kiRobot: KiTestRobot? = null
    robot<KiTestRobot>(rootScope) { kiRobot = this }

    assertFailsWith<Exception> { robot<MetroTestRobot>(rootScope) {} }

    assertThat(kiRobot).isNotNull()
  }

  @Test
  fun `a robot is provided for metro alone`() {
    val rootScope = rootScope(kiRobot = null, metroRobot = MetroTestRobot())

    assertFailsWith<Exception> { robot<KiTestRobot>(rootScope) {} }

    var metroRobot: MetroTestRobot? = null
    robot<MetroTestRobot>(rootScope) { metroRobot = this }

    assertThat(metroRobot).isNotNull()
  }

  @Test
  fun `a robot is provided for kotlin-inject and metro simultaneously`() {
    val rootScope = rootScope(kiRobot = KiTestRobot(), metroRobot = MetroTestRobot())

    var kiRobot: KiTestRobot? = null
    robot<KiTestRobot>(rootScope) { kiRobot = this }

    var metroRobot: MetroTestRobot? = null
    robot<MetroTestRobot>(rootScope) { metroRobot = this }

    assertThat(kiRobot).isNotNull()
    assertThat(metroRobot).isNotNull()
  }

  private fun rootScope(
    kiRobot: Robot? = KiTestRobot(),
    metroRobot: Robot? = MetroTestRobot(),
  ): Scope = Scope.buildRootScope {
    if (kiRobot != null) {
      addKotlinInjectComponent(Component(kiRobot))
    }
    if (metroRobot != null) {
      addMetroDependencyGraph(Graph(metroRobot))
    }
  }

  private class Component(vararg robots: Robot) : RobotComponent {
    override val robots: Map<KClass<out Robot>, () -> Robot> = robots.associate { robot ->
      robot::class to { robot }
    }
  }

  private class Graph(vararg robots: Robot) : RobotGraph {
    override val robots: Map<KClass<*>, () -> Robot> = robots.associate { robot ->
      robot::class to { robot }
    }
  }

  private class KiTestRobot : Robot {
    var closeCalled = false
      private set

    override fun close() {
      closeCalled = true
    }
  }

  private class MetroTestRobot : Robot {
    var closeCalled = false
      private set

    override fun close() {
      closeCalled = true
    }
  }
}
