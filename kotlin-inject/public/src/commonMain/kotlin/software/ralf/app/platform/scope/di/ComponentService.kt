package software.ralf.app.platform.scope.di

import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.parents

@PublishedApi internal const val DI_COMPONENT_KEY: String = "diComponent"

/** This function is deprecated. [kotlinInjectComponent] is a one to one replacement. */
@Deprecated(
  message = "Use kotlinInjectComponent instead.",
  replaceWith = ReplaceWith("kotlinInjectComponent<T>()"),
  level = DeprecationLevel.WARNING,
)
public inline fun <reified T : Any> Scope.diComponent(): T = kotlinInjectComponent()

/**
 * Provides the DI component that has been added to this [Scope]. A common pattern is to use this
 * function to look up component interfaces in static contexts like test methods, static functions
 * or where constructor injection cannot be used, e.g.
 *
 * ```
 * interface HudComponent {
 *     fun hudManager(): HudManager
 * }
 *
 * rootScope.diComponent<HudComponent>().hudManager()
 * ```
 *
 * The given component type [T] of the DI component can be provided by this scope or a parent scope.
 */
public inline fun <reified T : Any> Scope.kotlinInjectComponent(): T {
  parents(includeSelf = true)
    .firstNotNullOfOrNull { scope -> scope.getService<Any>(DI_COMPONENT_KEY) as? T }
    ?.let {
      return it
    }

  val diComponents =
    parents(includeSelf = true)
      .map { it.getService<Any>(DI_COMPONENT_KEY) }
      .filterNotNull()
      .map { it::class }

  // The replace() will align inner class references across platforms. Native uses a '.',
  // whereas the JVM platform use '$'.
  throw NoSuchElementException(
    "Couldn't find component implementing ${T::class}. Inspected: " +
      "[${diComponents.joinToString { it.simpleName.toString() }}] (fully qualified " +
      "names: [${diComponents.joinToString { it.toString().replace('\$', '.') }}])"
  )
}

/** This function is deprecated. [addKotlinInjectComponent] is a one to one replacement. */
@Deprecated(
  message = "Use addKotlinInjectComponent instead.",
  replaceWith = ReplaceWith("addKotlinInjectComponent(component)"),
  level = DeprecationLevel.WARNING,
)
public fun Scope.Builder.addDiComponent(component: Any) {
  addKotlinInjectComponent(component)
}

/**
 * Adds the given [component] to this builder. The instance can be later retrieved with
 * [kotlinInjectComponent].
 */
public fun Scope.Builder.addKotlinInjectComponent(component: Any) {
  addService(DI_COMPONENT_KEY, component)
}
