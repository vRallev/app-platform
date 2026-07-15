package software.ralf.app.platform.robot

import kotlin.reflect.KClass
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.kotlinInjectComponent
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/**
 * Test robots are an abstraction between test interactions and the underlying implementation. They
 * hide implementation details of features, mock implementations and the testing framework itself.
 * Instead of a test finding UI elements on screen and making assertions on them using UI test
 * frameworks, a robot would hide these details and do the heavy lifting. The robot can be shared
 * and reused across tests.
 *
 * This is the base class for all instrumentation test robots. Call [robot] to obtain a concrete
 * robot instance and invoke operations on it. Every time a new robot is requested, a new instance
 * is being created.
 *
 * A [Robot] must be annotated with [ContributesRobot]:
 * ```
 * @ContributesRobot(AppScope::class)
 * class ItineraryRobot : Robot {
 *   fun seeItinerary() = ...
 * }
 *
 * robot<ItineraryRobot> { seeItinerary() }
 * ```
 *
 * Using an `@Inject` constructor allows you to inject objects from the kotlin-inject object graph:
 * ```
 * @Inject
 * @ContributesRobot(AppScope::class)
 * class LoginRobot(
 *   private val authService: MockAuthenticationService
 * ) : Robot {
 *   fun login() {
 *     authService.loginMockAccount()
 *   }
 * }
 * ```
 *
 * The [robot] function instantiates a new robot with every call. To make a robot stateful and only
 * ever have a single instance you must wrap your test with the [robot] function:
 * ```
 * @Test
 * fun single_robot_instance() {
 *     robot<SingletonRobot> {
 *         robot<OtherRobot1> { .. }
 *         robot<OtherRobot2> { .. }
 *     }
 * }
 * ```
 *
 * The [close] function can be overridden if the robot manages resources that must be released.
 * [close] is invoked when the [robot] function returns.
 */
public interface Robot {
  /**
   * Closes this robot.
   *
   * This function should be overridden if the robot manages resources that must be released. This
   * function is invoked when the [robot] function returns and doesn't need to called manually.
   */
  // Default no-op implementation.
  public fun close(): Unit = Unit
}

/**
 * Creates a [Robot] of type [T] and invokes [block] on the newly created robot:
 * ```
 * @ContributesRobot
 * class AboutScreenRobot : Robot {
 *   fun assertAboutScreenUi() = ...
 * }
 *
 * robot<AboutScreenRobot> {
 *   assertAboutScreenUi()
 * }
 * ```
 *
 * [rootScope] refers to the application scope, which provides [RobotComponent]. Usually, the
 * parameter doesn't need to be changed and the default value can be used.
 */
public inline fun <reified T : Robot> robot(
  rootScope: Scope = software.ralf.app.platform.robot.internal.rootScope,
  noinline block: T.() -> Unit,
) {
  val robot = rootScope.allRobots[T::class]?.invoke() as? T

  checkNotNull(robot) {
    "Could not find Robot of type ${T::class}. Did you forget to add the @ContributesRobot " +
      "annotation?"
  }

  try {
    block(robot)
  } finally {
    robot.close()
  }
}

@PublishedApi
internal val Scope.allRobots: Map<KClass<*>, () -> Robot>
  get() {
    return kotlinInjectComponentOrNull<RobotComponent>()?.robots.orEmpty() +
      metroDependencyGraphOrNull<RobotGraph>()?.robots.orEmpty().mapValues { { it.value() } }
  }

private inline fun <reified T : Any> Scope.metroDependencyGraphOrNull(): T? {
  return try {
    metroDependencyGraph<T>()
  } catch (_: NoSuchElementException) {
    null
  }
}

private inline fun <reified T : Any> Scope.kotlinInjectComponentOrNull(): T? {
  return try {
    kotlinInjectComponent<T>()
  } catch (_: NoSuchElementException) {
    null
  }
}
