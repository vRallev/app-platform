package software.ralf.app.platform.scope.coroutine

import kotlin.concurrent.Volatile
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable

/**
 * Defers dispatch while [registration] has an open batch, then uses [delegate] to resume work.
 * Wrappers for a scope and its children share the same [Registration].
 *
 * Already-running work and tasks already submitted to [delegate] are not paused. Releasing a batch
 * allows its coroutines to run; it does not guarantee that collectors have subscribed.
 */
@OptIn(InternalCoroutinesApi::class)
internal open class RegistrationDispatcher
private constructor(
  private val delegate: CoroutineDispatcher,
  private val registration: Registration,
) : CoroutineDispatcher() {

  // Main.immediate and Unconfined must also enter dispatch() while the barrier is closed.
  // Otherwise, coroutine builders could start their bodies inline and skip the barrier.
  override fun isDispatchNeeded(context: CoroutineContext): Boolean =
    registration.completion != null || delegate.isDispatchNeeded(context)

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val dispatch = {
      // Recheck on release: completion can run on a different thread from this dispatch call.
      if (delegate.isDispatchNeeded(context)) {
        delegate.dispatch(context, block)
      } else {
        // Running the block directly can recurse when Unconfined coroutines yield or resume.
        // Enter the delegate's coroutine event loop instead; no additional Job is created.
        suspend {
            block.run()
          }
          .startCoroutine(
            Continuation(delegate) {
              it.getOrThrow()
            }
          )
      }
    }
    // Capture one batch. If it finishes before the handler is registered, Job invokes the
    // handler immediately, so dispatch cannot be lost between reading and waiting.
    // Release canceled tasks too: their continuations still need to process cancellation.
    val completion = registration.completion
    if (completion == null) {
      dispatch()
    } else {
      completion.invokeOnCompletion { dispatch() }
    }
  }

  override fun dispatchYield(context: CoroutineContext, block: Runnable) {
    // Preserve the delegate's yield scheduling when possible. A batch can finish after
    // isDispatchNeeded() forced dispatch, leaving out the YieldContext Unconfined expects.
    // Route immediate execution through dispatch() so this race still enters its event loop.
    if (registration.completion == null && delegate.isDispatchNeeded(context)) {
      delegate.dispatchYield(context, block)
    } else {
      dispatch(context, block)
    }
  }

  override fun toString(): String = "RegistrationDispatcher($delegate)"

  companion object {
    fun create(
      delegate: CoroutineDispatcher,
      registration: Registration,
    ): CoroutineDispatcher {
      return if (delegate is Delay) {
        DelayingDispatcher(delegate, registration, delegate)
      } else {
        RegistrationDispatcher(delegate, registration)
      }
    }
  }

  /**
   * Shared barrier for nested registration batches. Pair every [begin] with [end], including when a
   * callback throws. Only the outermost [end] releases deferred work.
   *
   * Calls to [begin] and [end] must be serialized on the registration thread. Coroutine workers may
   * read [completion] and attach handlers, but this does not make scope registration itself safe to
   * call concurrently.
   */
  internal class Registration {
    // null allows dispatch; an incomplete Job holds it. This Job is only a completion signal,
    // independent of the scope's lifecycle Job. Volatile publishes it to dispatcher threads.
    @Volatile
    var completion: CompletableJob? = null
      private set

    private var depth = 0

    fun begin() {
      if (depth++ == 0) {
        completion = Job()
      }
    }

    fun end() {
      if (--depth == 0) {
        val pending = completion
        // Open the barrier first. Completion handlers can run inline, dispatch more work,
        // or begin another registration batch without inheriting this completed batch.
        completion = null
        pending?.complete()
      }
    }
  }

  // Keep a TestDispatcher's virtual clock and a Main dispatcher's timers. Dispatchers without
  // Delay continue to use kotlinx.coroutines' default timer implementation.
  private class DelayingDispatcher(
    delegate: CoroutineDispatcher,
    registration: Registration,
    delay: Delay,
  ) : RegistrationDispatcher(delegate, registration), Delay by delay
}
