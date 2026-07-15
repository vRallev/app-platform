package software.ralf.app.platform.scope

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped
import software.ralf.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.ralf.app.platform.scope.coroutine.coroutineScope

/**
 * It's recommended to use this builder instead of [Scope.buildRootScope] in unit tests.
 *
 * This builder creates a new root scope similar to [Scope.buildRootScope], but also adds a
 * coroutine scope automatically to the returned scope to support the [coroutineScope] extension.
 * The coroutine scope uses [StandardTestDispatcher] by default. Use [context] to override any
 * element of coroutine scope, e.g. to use the unconfined behavior you can pass in
 * [UnconfinedTestDispatcher]:
 * ```
 * @Test
 * fun `my test`() = runTest {
 *   val scope = Scope.buildTestScope(this, context = UnconfinedTestDispatcher())
 * }
 * ```
 *
 * If you don't rely on the coroutine runtime in your test or you don't have a parameter value for
 * [testScope], then you can safely use [Scope.buildRootScope] as well.
 *
 * ## Clean up
 *
 * When the given [testScope] is canceled, which usually happens after the test body returns, then
 * the returned scope is destroyed through [Scope.destroy] automatically if it hasn't been destroyed
 * already. This also means [Scoped.onExitScope] will be called for all registered instances.
 *
 * ## Convenience
 *
 * Consider using [runTestWithScope] as a convenience to avoid creating a [Scope] manually:
 * ```
 * @Test
 * fun `my test`() = runTestWithScope { scope ->
 *   ...
 * }
 * ```
 */
public fun Scope.Companion.buildTestScope(
  testScope: TestScope,
  name: String = "test",
  context: CoroutineContext = EmptyCoroutineContext,
  builder: (Scope.Builder.() -> Unit)? = null,
): Scope {
  val baseContext = testScope.backgroundScope.coroutineContext

  val coroutineContext =
    baseContext + SupervisorJob(baseContext.job) + CoroutineName(name) + context

  val scope =
    buildRootScope(name = name) {
      addCoroutineScopeScoped(CoroutineScopeScoped(coroutineContext))
      builder?.invoke(this)
    }

  baseContext.job.invokeOnCompletion { scope.destroy() }

  return scope
}
