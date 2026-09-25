package software.ralf.app.platform.scope.coroutine

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped

internal const val COROUTINE_SCOPE_KEY = "coroutineScope"

private val Scope.coroutineScopeScoped: CoroutineScopeScoped
  get() {
    val result =
      checkNotNull(getService<CoroutineScopeScoped>(COROUTINE_SCOPE_KEY)) {
        "Couldn't find CoroutineScopeScoped within scope $name."
      }
    check(result.isActive) {
      "Expected the coroutine scope ${result.coroutineContext[CoroutineName]?.name} still " +
        "to be active."
    }
    return result
  }

/**
 * Returns a coroutine scope bound to the lifecycle of this [Scope]. It's not necessary nor
 * recommended to cancel the returned scope as this automatically happens when the [Scope] is being
 * destroyed.
 *
 * The [CoroutineScope] uses IO dispatcher by default and launched jobs run on a background thread.
 *
 * Jobs created by this scope don't need to be canceled.
 *
 * **Note:** During builder or batch registration of multiple [Scoped] instances with
 * [Scope.register], work using this scope's dispatcher waits for the batch to finish registering.
 * Passing a dispatcher in [context] preserves this wait; replacing it in a later `launch` or
 * `async` call bypasses it.
 *
 * `CoroutineStart.UNDISPATCHED` also bypasses this wait: the coroutine runs immediately on the
 * calling thread until its first suspension. Later dispatched resumptions still wait for
 * registration to finish.
 *
 * ```kotlin
 * override fun onEnterScope(scope: Scope) {
 *   // Both calls wait until all Scoped instances are registered and all onEnterScope() functions
 *   // have been called before running the lambda.
 *   scope.launch(otherDispatcher) { }
 *   scope.coroutineScope(otherDispatcher).launch { }
 *
 *   // Does not wait until all Scoped instances have been registered.
 *   scope.coroutineScope().launch(otherDispatcher) { }
 *
 *   // Runs immediately until its first suspension.
 *   scope.launch(start = CoroutineStart.UNDISPATCHED) { }
 * }
 * ```
 */
public fun Scope.coroutineScope(context: CoroutineContext = EmptyCoroutineContext): CoroutineScope {
  return coroutineScopeScoped.createChild(context)
}

/**
 * Adds the given [coroutineScope] to the [Scope] that will be built. A child scope can be retrieved
 * with `coroutineScope()`.
 */
public fun Scope.Builder.addCoroutineScopeScoped(coroutineScope: CoroutineScopeScoped) {
  addService(COROUTINE_SCOPE_KEY, coroutineScope)
  register(coroutineScope)
}

/**
 * Launches a new job in the [CoroutineScope] created by [coroutineScope]. The job run on the IO
 * dispatcher by default. The lifecycle of the job is bound to the lifecycle of the [Scope] and
 * therefore doesn't need to be canceled. However, it's generally good practice to stop and cancel
 * ongoing background work eargerly.
 *
 * This is a short version of `coroutineScope(context).launch(start = start) { }`.
 * [CoroutineStart.UNDISPATCHED] runs immediately until the first suspension, bypassing the wait for
 * [Scoped] registration.
 *
 * See [coroutineScope] for more details.
 */
public fun Scope.launch(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit,
): Job {
  return coroutineScope(context).launch(start = start, block = block)
}

/**
 * Destroys this scope and its children, then waits for their coroutine jobs to finish, including
 * suspending cleanup. Waits for coroutine scopes added with [addCoroutineScopeScoped] and all their
 * child jobs. Scopes without a coroutine scope are also destroyed.
 *
 * Call from a coroutine outside the scopes being destroyed. Once called, destruction and the wait
 * finish even if the caller is canceled.
 *
 * This scope must not already be destroyed.
 */
public suspend fun Scope.destroyAndWait(): Unit =
  withContext(NonCancellable) {
    val jobs =
      generateSequence(listOf(this@destroyAndWait)) { scopes ->
          scopes.flatMap { it.children() }.takeIf { it.isNotEmpty() }
        }
        .flatten()
        .mapNotNull { scope ->
          scope.getService<CoroutineScopeScoped>(COROUTINE_SCOPE_KEY)?.coroutineContext?.job
        }
        .toList()

    destroy()
    jobs.joinAll()
  }
