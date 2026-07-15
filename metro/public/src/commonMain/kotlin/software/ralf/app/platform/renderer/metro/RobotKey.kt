package software.ralf.app.platform.renderer.metro

import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/**
 * DO NOT USE DIRECTLY.
 *
 * This is a multibindings key used in Metro for identifying robots by their type. This key is used
 * by our custom code generator for `@ContributesRobot`. [value] refers to the concrete `Robot`
 * type.
 */
@MapKey public annotation class RobotKey(val value: KClass<*>)
