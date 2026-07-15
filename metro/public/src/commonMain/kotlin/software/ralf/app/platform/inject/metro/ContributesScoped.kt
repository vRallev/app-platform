package software.ralf.app.platform.inject.metro

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass
import software.ralf.app.platform.scope.Scoped

/**
 * Used to contribute a class implementing the [Scoped] interface to the given [scope], e.g.
 *
 * ```
 * @Inject
 * @SingleIn(AppScope::class)
 * @ContributesScoped(AppScope::class)
 * class MyClass(..) : SuperType, Scoped
 * ```
 *
 * This annotation is a shortcut for using `@ContributesBinding` and `@ContributesIntoSet`, but with
 * a qualifier for the multibinding alone. This can in Metro only be expressed with a contributed
 * graph:
 * ```
 * @Inject
 * @SingleIn(AppScope::class)
 * class MyClass(..) : SuperType, Scoped
 *
 * @ContributesTo(AppScope::class)
 * interface MyClassGraph {
 *   @Binds val MyClass.bindSuperType: SuperType
 *
 *   @Binds @IntoSet @ForScope(AppScope::class) val MyClass.bindScoped: Scoped
 * }
 * ```
 *
 * Note that this annotation is only applicable for Metro and not kotlin-inject, because for
 * kotlin-inject we provide a custom code generator out of the box when using `@ContributesBinding`
 * that can handle the [Scoped] multibinding interface.
 */
@Target(CLASS)
public annotation class ContributesScoped(
  /** The scope in which to include this contributed binding. */
  val scope: KClass<*>
)
