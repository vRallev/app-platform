package software.ralf.app.platform.inject.robot

import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation

/**
 * Robots must be annotated with `@ContributesRobot`. The annotation will generate the necessary
 * code to provide the robot in the dependency graph and allow us to retrieve the robot through the
 * `robot<AbcRobot> { }` function.
 *
 * ```
 * @ContributesRobot(AppScope::class)
 * class AbcRobot : Robot {
 *     ...
 * }
 * ```
 *
 * It's supported to inject dependencies in the constructor. For this the class must be annotated
 * with `@Inject`:
 * ```
 * @Inject
 * @ContributesRobot(AppScope::class)
 * class AbcRobot(
 *     val someDependency: Dependency,
 * ) : Robot() {
 *     ...
 * }
 * ```
 */
@ContributingAnnotation
public annotation class ContributesRobot(
  /** The scope in which to include this contributed binding. */
  val scope: KClass<*>
)
