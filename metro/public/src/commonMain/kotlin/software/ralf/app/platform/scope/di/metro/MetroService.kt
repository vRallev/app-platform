package software.ralf.app.platform.scope.di.metro

import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.parents

@PublishedApi internal const val METRO_DEPENDENCY_GRAPH_KEY: String = "metroDependencyGraph"

/**
 * Provides the Metro dependency graph that has been added to this [Scope]. A common pattern is to
 * use this function to look up graph interfaces in static contexts like test methods, static
 * functions or where constructor injection cannot be used, e.g.
 *
 * ```
 * interface HudGraph {
 *     val hudManager: HudManager
 * }
 *
 * rootScope.metroDependencyGraph<HudGraph>().hudManager
 * ```
 *
 * The given graph type [T] of the DI graph can be provided by this scope or a parent scope.
 */
public inline fun <reified T : Any> Scope.metroDependencyGraph(): T {
  parents(includeSelf = true)
    .firstNotNullOfOrNull { scope -> scope.getService<Any>(METRO_DEPENDENCY_GRAPH_KEY) as? T }
    ?.let {
      return it
    }

  val diGraphs =
    parents(includeSelf = true)
      .map { it.getService<Any>(METRO_DEPENDENCY_GRAPH_KEY) }
      .filterNotNull()
      .map { it::class }

  // The replace() will align inner class references across platforms. Native uses a '.',
  // whereas the JVM platform use '$'.
  throw NoSuchElementException(
    "Couldn't find dependency graph implementing ${T::class}. Inspected: " +
      "[${diGraphs.joinToString { it.simpleName.toString() }}] (fully qualified " +
      "names: [${diGraphs.joinToString { it.toString().replace('\$', '.') }}])"
  )
}

/**
 * Adds the given [dependencyGraph] to this builder. The instance can be later retrieved with
 * [metroDependencyGraph].
 */
public fun Scope.Builder.addMetroDependencyGraph(dependencyGraph: Any) {
  addService(METRO_DEPENDENCY_GRAPH_KEY, dependencyGraph)
}
