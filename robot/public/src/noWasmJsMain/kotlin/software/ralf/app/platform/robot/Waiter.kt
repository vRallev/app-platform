package software.ralf.app.platform.robot

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private val defaultTimeout = 10.seconds
private val defaultDelay = 15.milliseconds

/**
 * Blocks the current thread until the given suspending [block] returns true or the [timeout]
 * occurs. [block] is invoked multiple times with the given [delay] to check the condition. The
 * timeout includes time spent inside [block] and waiting between attempts. In case of a timeout an
 * [IllegalStateException] is thrown, because the app never transitioned into the expected state.
 * For better error messages [condition] describes what [block] is checking and waiting for.
 *
 * Note that this function should not be called from the main thread. The most common use case is
 * calling it from the instrumentation test thread that is used by default in the test function. The
 * thread this function is invoked in gets blocked and not suspended like a coroutine.
 */
public fun waitUntil(
  condition: String,
  timeout: Duration = defaultTimeout,
  delay: Duration = defaultDelay,
  block: suspend () -> Boolean,
) {
  runBlocking {
    val succeeded =
      withTimeoutOrNull(timeout) {
        while (!block()) {
          delay(delay)
        }

        true
      }

    check(succeeded == true) { "Waiting until '$condition' never returned true." }
  }
}

/**
 * Similar to [waitUntil], but allows the suspending [block] to throw any error when the condition
 * isn't met. Coroutine cancellation is not retried. The last failed attempt is included as the
 * cause when waiting times out. This is helpful for example to wait for a UI element, e.g.
 *
 * ```
 * waitUntilCatching("text is visible") {
 *     seeViewWithText("Some text")
 * }
 * ```
 */
@Suppress("TooGenericExceptionCaught")
public fun waitUntilCatching(
  condition: String,
  timeout: Duration = defaultTimeout,
  delay: Duration = defaultDelay,
  block: suspend () -> Unit,
) {
  var lastException: Throwable? = null
  try {
    waitUntil(condition = condition, timeout = timeout, delay = delay) {
      try {
        block()
        true
      } catch (exception: CancellationException) {
        throw exception
      } catch (t: Throwable) {
        lastException = t
        false
      }
    }
  } catch (t: Throwable) {
    throw t as? CancellationException
      ?: IllegalStateException("Waiting until '$condition' never succeeded.", lastException ?: t)
  }
}

/**
 * Blocks the current thread until the suspending [block] returns a non-null value or [timeout]
 * occurs. [block] is invoked multiple times with the given [delay] while it returns null. The
 * timeout includes time spent inside [block] and waiting between attempts, e.g.
 *
 * ```
 * val session = waitFor("user is authenticated") {
 *     sessionManager.sessionFlow.first { it is AuthSession.Authenticated }
 * }
 * ```
 */
@Suppress("TooGenericExceptionCaught")
public fun <T : Any> waitFor(
  condition: String,
  timeout: Duration = defaultTimeout,
  delay: Duration = defaultDelay,
  block: suspend () -> T?,
): T {
  var result: T? = null

  try {
    waitUntil(condition = condition, timeout = timeout, delay = delay) {
      result = block()
      result != null
    }
  } catch (t: Throwable) {
    throw if (t is CancellationException || result != null) {
      t
    } else {
      IllegalStateException(
        "Waiting for '$condition' never succeeded and the value is null.",
        t,
      )
    }
  }

  return checkNotNull(result) { "Waiting for '$condition' never succeeded and the value is null." }
}
