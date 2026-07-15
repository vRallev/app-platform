// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

@SingleIn(AppScope::class)
<!CONTRIBUTES_ROBOT_ERROR!>@ContributesRobot(AppScope::class)<!>
class TestRobot : Robot
