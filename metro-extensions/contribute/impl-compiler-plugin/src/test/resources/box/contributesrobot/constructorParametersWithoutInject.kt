package com.test

import dev.zacsweers.metro.BindingContainer
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot
import software.ralf.app.platform.robot.RobotGraph

class RobotDependency {
  fun value(): String = "dependency"
}

@ContributesRobot(AppScope::class)
class TestRobot(
  val dependency: RobotDependency,
) : Robot

@DependencyGraph(AppScope::class)
interface MyGraph : RobotGraph {
  @Provides fun robotDependency(): RobotDependency = RobotDependency()
}

fun box(): String {
  if (
    TestRobot.RobotContribution::class.java.getAnnotation(BindingContainer::class.java) == null
  ) {
    return "FAIL: expected RobotContribution to be a BindingContainer"
  }
  if (
    TestRobot.RobotContribution::class.java.declaredMethods.any { it.name == "provideTestRobot" }
  ) {
    return "FAIL: expected provider to be moved off the RobotContribution interface"
  }
  if (
    TestRobot.RobotContribution.Companion::class.java.declaredMethods.none {
      it.name == "provideTestRobot"
    }
  ) {
    return "FAIL: expected provider on RobotContribution companion"
  }

  val graph = createGraph<MyGraph>()
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
