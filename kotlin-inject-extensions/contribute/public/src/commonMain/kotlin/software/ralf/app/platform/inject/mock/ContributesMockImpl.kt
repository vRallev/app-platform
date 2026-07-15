package software.ralf.app.platform.inject.mock

import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation
import software.ralf.app.platform.scope.Scoped

/**
 * Used to contribute a mocked implementation to a given interface that has a real implementation as
 * well.
 *
 * ```
 * @ContributesMockImpl(AppScope::class)
 * @Inject
 * @SingleIn(AppScope::class)
 * class MockVts : Vts
 * ```
 *
 * This annotation will generate the following kotlin-inject component:
 * ```
 * @ContributesTo(AppScope::class)
 * interface MockVtsMockComponent {
 *      @Provides
 *      fun provideTestVts(
 *          @MockMode mockMode: Boolean,
 *          mockVts: () -> MockVts,
 *          @RealImpl realVts: () -> Vts,
 *       ): Vts = if (mockMode) mockVts() else realVts()
 * }
 * ```
 *
 * This annotation is also repeatable:
 * ```
 * @ContributesMockImpl(AppScope::class, boundType = Vts::class)
 * @ContributesMockImpl(AppScope::class, boundType = Vts2::class)
 * class MockVts : Vts, Vts2
 * ```
 *
 * It is safe to implement the [Scoped] interface. [Scoped.onEnterScope] and [Scoped.onExitScope]
 * will only be called if the mock implementation is used at runtime:
 * ```
 * @ContributesMockImpl(AppScope::class)
 * @Inject
 * @SingleIn(AppScope::class)
 * class MockVts : Vts, Scoped
 * ```
 */
@Repeatable
@ContributingAnnotation
public annotation class ContributesMockImpl(
  /** The scope in which to include this contributed binding. */
  val scope: KClass<*>,

  /**
   * The type that this class is bound to, this is required when there is more than a single
   * superType or the superType is not an interface.
   */
  val boundType: KClass<*> = Unit::class,
)
