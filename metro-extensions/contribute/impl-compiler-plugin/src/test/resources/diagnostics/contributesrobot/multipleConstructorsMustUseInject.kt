// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

class RobotDependency

<!CONTRIBUTES_ROBOT_ERROR!>@ContributesRobot(AppScope::class)<!>
class TestRobot(
  val dependency: RobotDependency,
) : Robot {
  constructor(dependency: RobotDependency, marker: String) : this(dependency)
}
