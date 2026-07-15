package software.ralf.app.platform.scope.coroutine

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import software.ralf.app.platform.scope.Scope

private const val COROUTINE_SCOPE_KEY = "coroutineScope"

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
 * This is a short version of `coroutineScope().launch { }`.
 *
 * See [coroutineScope] for more details.
 */
public fun Scope.launch(
  context: CoroutineContext = EmptyCoroutineContext,
  block: suspend CoroutineScope.() -> Unit,
): Job {
  return coroutineScope(context).launch(block = block)
}
