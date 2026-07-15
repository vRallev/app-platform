package software.ralf.test

import kotlin.reflect.KClass
import software.ralf.app.platform.robot.Robot

interface TestRobotGraph {
  val robots: Map<KClass<*>, () -> Robot>
}
