package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot
import software.ralf.app.platform.robot.RobotGraph

@Inject
class RobotDependency {
  fun value(): String = "dependency"
}

@Inject
@ContributesRobot(AppScope::class)
class TestRobot(
  val dependency: RobotDependency,
) : Robot

@DependencyGraph(AppScope::class)
interface MyGraph

fun box(): String {
  val graph = createGraph<MyGraph>() as RobotGraph
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  if (robot !is TestRobot) {
    return "FAIL: expected TestRobot but got $robot"
  }

  return if (robot.dependency.value() == "dependency") {
    "OK"
  } else {
    "FAIL: dependency was not injected"
  }
}
