package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot
import software.ralf.app.platform.robot.RobotGraph

abstract class TestScope private constructor()

@ContributesRobot(TestScope::class)
class TestRobot : Robot

@DependencyGraph(TestScope::class)
interface MyGraph

fun box(): String {
  val graph = createGraph<MyGraph>() as RobotGraph
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  return if (robot is TestRobot) "OK" else "FAIL: expected TestRobot but got $robot"
}
