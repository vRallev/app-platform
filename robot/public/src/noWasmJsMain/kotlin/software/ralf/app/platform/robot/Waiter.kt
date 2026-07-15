package software.ralf.app.platform.robot

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private val defaultTimeout = 10.seconds
private val defaultDelay = 15.milliseconds

/**
 * Blocks the current thread until the given [block] returns true or the [timeout] occurs. [block]
 * is invoked multiple times with the given [delay] to check the condition. In case of a timeout an
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
  block: () -> Boolean,
) {
  val sleepCycles = (timeout / delay).toInt()

  runBlocking {
    repeat(sleepCycles) {
      if (!block()) {
        delay(delay)
      } else {
        return@runBlocking
      }
    }

    check(block()) { "Waiting until '$condition' never returned true." }
  }
}

/**
 * Similar to [waitUntil], but allows [block] to throw any error when the condition isn't met. This
 * is helpful for example to wait for a UI element, e.g.
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
  block: () -> Unit,
) {
  var lastException: Throwable? = null
  try {
    waitUntil(condition = condition, timeout = timeout, delay = delay) {
      try {
        lastException = null
        block()
        true
      } catch (t: Throwable) {
        lastException = t
        false
      }
    }
  } catch (t: Throwable) {
    if (lastException != null) {
      throw IllegalStateException("Waiting until '$condition' never succeeded.", lastException)
    } else {
      throw t
    }
  }
}

/**
 * Similar to [waitUntil], but allows [block] to throw any error when the condition isn't met. This
 * is helpful for example to wait for a UI element, e.g.
 *
 * ```
 * waitUntilCatching("text is visible") {
 *     seeViewWithText("Some text")
 * }
 * ```
 */
@Suppress("TooGenericExceptionCaught")
public fun <T : Any> waitFor(
  condition: String,
  timeout: Duration = defaultTimeout,
  delay: Duration = defaultDelay,
  block: () -> T?,
): T {
  var result: T? = null

  try {
    waitUntil(condition = condition, timeout = timeout, delay = delay) {
      result = block()
      result != null
    }
  } catch (t: Throwable) {
    if (result == null) {
      throw IllegalStateException(
        "Waiting for '$condition' never succeeded and the value is null.",
        t,
      )
    } else {
      throw t
    }
  }

  return checkNotNull(result) { "Waiting for '$condition' never succeeded and the value is null." }
}
