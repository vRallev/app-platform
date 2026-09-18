package software.ralf.app.platform.scope.coroutine

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped

/**
 * A [CoroutineScope] that can be registered with a [Scope] to be automatically canceled when the
 * [Scope] gets destroyed.
 */
public class CoroutineScopeScoped(coroutineContext: CoroutineContext) : CoroutineScope, Scoped {

  internal val registration = RegistrationDispatcher.Registration()

  override val coroutineContext: CoroutineContext = coroutineContext.withRegistrationDispatcher()

  private val parentName =
    requireNotNull(coroutineContext[CoroutineName]) {
      "Expected the coroutine context to have a name."
    }

  override fun onExitScope() {
    coroutineContext.cancel()
  }

  /**
   * Creates a child [CoroutineScope] with this scope as parent. Canceling the child does not cancel
   * this scope or its other children.
   */
  public fun createChild(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ): CoroutineScope {
    val name = coroutineContext[CoroutineName] ?: CoroutineName(parentName.name + "-child")
    val childContext = this.coroutineContext + coroutineContext
    // Inherited dispatchers already share our barrier. Wrap only an explicit replacement,
    // using the same Registration so this child also waits for the scope's batch to finish.
    val context =
      if (coroutineContext[ContinuationInterceptor] != null) {
        childContext.withRegistrationDispatcher()
      } else {
        childContext
      }

    return CoroutineScope(context + Job(this.coroutineContext.job) + name)
  }

  private fun CoroutineContext.withRegistrationDispatcher(): CoroutineContext {
    val interceptor = this[ContinuationInterceptor]
    require(interceptor == null || interceptor is CoroutineDispatcher) {
      "Expected a CoroutineDispatcher for scoped coroutines."
    }
    // With no dispatcher, launch/async normally add Dispatchers.Default. Our wrapper fills
    // the ContinuationInterceptor slot, so those builders no longer supply that fallback.
    // Choose Default as the delegate here: it uses a shared worker pool on JVM and Native.
    // A child with no dispatcher override inherits its parent's wrapper instead of using this
    // fallback, so an IO or Main parent stays on that dispatcher.
    val dispatcher = interceptor ?: Dispatchers.Default
    return this + RegistrationDispatcher.create(dispatcher, registration)
  }
}
