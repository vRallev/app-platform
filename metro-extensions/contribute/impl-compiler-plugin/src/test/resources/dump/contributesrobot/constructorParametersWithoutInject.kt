// RUN_PIPELINE_TILL: BACKEND
package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

class RobotDependency

class AnotherRobotDependency

@ContributesRobot(AppScope::class)
class TestRobot(
  val dependency: RobotDependency,
  val anotherDependency: AnotherRobotDependency,
) : Robot
