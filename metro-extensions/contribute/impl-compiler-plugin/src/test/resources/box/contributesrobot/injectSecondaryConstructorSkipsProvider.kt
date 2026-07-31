package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot
import software.ralf.app.platform.robot.RobotGraph

@Inject
class RobotDependency {
  fun value(): String = "dependency"
}

@ContributesRobot(AppScope::class)
class TestRobot private constructor(
  val dependency: RobotDependency,
  val marker: String,
) : Robot {
  @Inject constructor(dependency: RobotDependency) : this(dependency, "injected")
}

@DependencyGraph(AppScope::class)
interface MyGraph : RobotGraph

fun box(): String {
  val graph = createGraph<MyGraph>()
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  if (robot !is TestRobot) {
    return "FAIL: expected TestRobot but got $robot"
  }

  return if (robot.dependency.value() == "dependency" && robot.marker == "injected") {
    "OK"
  } else {
    "FAIL: dependency was not injected through secondary constructor"
  }
}
