// RUN_PIPELINE_TILL: BACKEND
// DUMP_KT_IR
package com.test

import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

@ContributesRobot(AppScope::class)
class TestRobot : Robot
