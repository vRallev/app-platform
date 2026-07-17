package software.ralf.app.platform.template.navigation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.launch

/**
 * A scoped service that continuously generates random values and feeds them into
 * [ExampleRepository] every 3 seconds. This is active only while the [AppScope] is alive.
 *
 * This class is:
 * - Bound to [AppScope] via `@ContributesScoped`
 * - A singleton within that scope via `@SingleIn`
 * - Injected via constructor using `@Inject`
 *
 * The generator starts emitting random integers in the range 1 to 100 as soon as the scope is
 * entered.
 *
 * @property exampleRepository the repository where generated values are pushed
 */
@Inject
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class ExampleValueGenerator(private val exampleRepository: ExampleRepository) : Scoped {
  override fun onEnterScope(scope: Scope) {
    scope.launch {
      while (true) {
        val random = (1..100).random()
        println("random: $random")
        exampleRepository.setExampleFlowValue(random)
        delay(3000L)
      }
    }
  }
}
