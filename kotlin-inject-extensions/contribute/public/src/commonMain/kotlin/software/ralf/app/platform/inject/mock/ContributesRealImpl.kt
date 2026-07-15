package software.ralf.app.platform.inject.mock

import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation
import software.ralf.app.platform.scope.Scoped

/**
 * Used to contribute a real implementation to a given interface that has a mocked implementation as
 * well.
 *
 * ```
 * @ContributesRealImpl(AppScope::class)
 * @Inject
 * @SingleIn(AppScope::class)
 * class RealVts : Vts
 * ```
 *
 * This annotation will generate the following kotlin-inject component:
 * ```
 * @ContributesTo(AppScope::class)
 * interface RealVtsRealImplComponent {
 *      @Provides
 *      @RealImpl
 *      fun provideVts(realVts: RealVts): Vts = realVts
 * }
 * ```
 *
 * This annotation is also repeatable, where for each bound type a provider method will be
 * generated:
 * ```
 * @ContributesRealImpl(AppScope::class, boundType = Vts::class)
 * @ContributesRealImpl(AppScope::class, boundType = Vts2::class)
 * class RealVts : Vts, Vts2
 * ```
 *
 * It is safe to implement the [Scoped] interface. [Scoped.onEnterScope] and [Scoped.onExitScope]
 * will only be called if the real implementation is used at runtime:
 * ```
 * @ContributesRealImpl(AppScope::class)
 * @Inject
 * @SingleIn(AppScope::class)
 * class RealVts : Vts, Scoped
 * ```
 */
@Repeatable
@ContributingAnnotation
public annotation class ContributesRealImpl(
  /** The scope in which to include this contributed binding. */
  val scope: KClass<*>,

  /**
   * The type that this class is bound to, this is required when there is more than a single
   * superType or the superType is not an interface.
   */
  val boundType: KClass<*> = Unit::class,
)
