package software.ralf.app.platform.scope.coroutine

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped

/**
 * A [CoroutineScope] that can be registered with a [Scope] to be automatically canceled when the
 * [Scope] gets destroyed.
 */
public class CoroutineScopeScoped(override val coroutineContext: CoroutineContext) :
  CoroutineScope, Scoped {

  private val parentName =
    requireNotNull(coroutineContext[CoroutineName]) {
      "Expected the coroutine context to have a name."
    }

  override fun onExitScope() {
    coroutineContext.cancel()
  }

  /** Creates a child [CoroutineScope] with this scope as parent. */
  public fun createChild(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ): CoroutineScope {
    val name = coroutineContext[CoroutineName] ?: CoroutineName(parentName.name + "-child")

    return CoroutineScope(
      this.coroutineContext + coroutineContext + Job(this.coroutineContext.job) + name
    )
  }
}
