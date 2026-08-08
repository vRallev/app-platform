package software.ralf.app.platform.robot

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.cause
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class WaiterTest {

  @Test
  fun `waitUntil blocks until the condition is met`() {
    val currentTime = System.currentTimeMillis()

    var counter = 0
    waitUntil(condition = "Wait for test condition", delay = 100.milliseconds) {
      counter++
      counter == 5
    }

    assertThat(counter).isEqualTo(5)
    assertThat(System.currentTimeMillis() - currentTime).isGreaterThan(399L)
  }

  @Test
  fun `waitUntil throws an error when the condition is never met`() {
    assertFailure {
        waitUntil(
          condition = "Wait for test condition",
          timeout = 200.milliseconds,
          delay = 100.milliseconds,
        ) {
          false
        }
      }
      .messageContains("Waiting until 'Wait for test condition' never returned true.")
  }

  @Test
  fun `waitUntil supports a suspending condition`() {
    var counter = 0

    waitUntil(condition = "Wait for suspending condition", delay = 10.milliseconds) {
      delay(10.milliseconds)
      ++counter == 3
    }

    assertThat(counter).isEqualTo(3)
  }

  @Test
  fun `waitUntil times out while the condition is suspended`() {
    val elapsed = measureTime {
      assertFailure {
          waitUntil(
            condition = "Wait for suspended condition",
            timeout = 100.milliseconds,
            delay = 10.milliseconds,
          ) {
            delay(5.seconds)
            true
          }
        }
        .messageContains("Waiting until 'Wait for suspended condition' never returned true.")
    }

    assertThat(elapsed).isLessThan(1.seconds)
  }

  @Test
  fun `waitUntil times out during the polling delay`() {
    val elapsed = measureTime {
      assertFailure {
          waitUntil(
            condition = "Wait between attempts",
            timeout = 100.milliseconds,
            delay = 5.seconds,
          ) {
            false
          }
        }
        .messageContains("Waiting until 'Wait between attempts' never returned true.")
    }

    assertThat(elapsed).isLessThan(1.seconds)
  }

  @Test
  fun `throwing an exception in waitUntil bubbles up`() {
    assertFailure {
        waitUntil(
          condition = "Wait for test condition",
          timeout = 200.milliseconds,
          delay = 100.milliseconds,
        ) {
          error("Test exception")
        }
      }
      .messageContains("Test exception")
  }

  @Test
  fun `waitUntil does not swallow coroutine cancellation`() {
    val cancellation = CancellationException("Test cancellation")

    assertFailure {
      waitUntil(condition = "Wait for canceled condition") { throw cancellation }
    }
      .all {
        isInstanceOf<CancellationException>()
        messageContains("Test cancellation")
      }
  }

  @Test
  fun `waitUntilCatching blocks until no exception is thrown`() {
    var counter = 0

    waitUntilCatching(
      condition = "Wait for test condition",
      timeout = 2.seconds,
      delay = 20.milliseconds,
    ) {
      counter++
      if (counter < 5) {
        error("Test exception")
      }
    }

    assertThat(counter).isEqualTo(5)
  }

  @Test
  fun `waitUntilCatching supports a suspending condition`() {
    var counter = 0

    waitUntilCatching(condition = "Wait for suspending condition", delay = 10.milliseconds) {
      delay(10.milliseconds)
      check(++counter == 3) { "Suspending condition is not ready." }
    }

    assertThat(counter).isEqualTo(3)
  }

  @Test
  fun `waitUntilCatching retries assertion failures until the assertion passes`() {
    var counter = 0

    waitUntilCatching(
      condition = "Wait for test assertion",
      timeout = 2.seconds,
      delay = 20.milliseconds,
    ) {
      assertThat(++counter).isEqualTo(5)
    }

    assertThat(counter).isEqualTo(5)
  }

  @Test
  fun `waitUntilCatching rethrows the original assertion failure after the timeout`() {
    val assertionError = AssertionError("Test assertion")

    assertFailure {
        waitUntilCatching(
          condition = "Wait for test assertion",
          timeout = 100.milliseconds,
          delay = 20.milliseconds,
        ) {
          throw assertionError
        }
      }
      .isSameInstanceAs(assertionError)
  }

  @Test
  fun `waitUntilCatching throws an error when condition is never met`() {
    assertFailure {
      waitUntilCatching(
        condition = "Wait for test condition",
        timeout = 200.milliseconds,
        delay = 20.milliseconds,
      ) {
        error("Test exception")
      }
    }
      .all {
        messageContains("Waiting until 'Wait for test condition' never succeeded.")
        cause().isNotNull().messageContains("Test exception")
      }
  }

  @Test
  fun `waitUntilCatching times out while the condition is suspended`() {
    val elapsed = measureTime {
      assertFailure {
          waitUntilCatching(
            condition = "Wait for suspended condition",
            timeout = 100.milliseconds,
            delay = 10.milliseconds,
          ) {
            delay(5.seconds)
          }
        }
        .messageContains("Waiting until 'Wait for suspended condition' never succeeded.")
    }

    assertThat(elapsed).isLessThan(1.seconds)
  }

  @Test
  fun `waitUntilCatching preserves the last failure when a later attempt times out`() {
    var counter = 0

    assertFailure {
      waitUntilCatching(
        condition = "Wait for suspended condition",
        timeout = 100.milliseconds,
        delay = 10.milliseconds,
      ) {
        if (counter++ == 0) {
          error("Last meaningful failure")
        }

        delay(5.seconds)
      }
    }
      .all {
        messageContains("Waiting until 'Wait for suspended condition' never succeeded.")
        cause().isNotNull().messageContains("Last meaningful failure")
      }
  }

  @Test
  fun `waitUntilCatching preserves the last assertion when a later attempt times out`() {
    val assertionError = AssertionError("Last meaningful assertion")
    var counter = 0

    assertFailure {
        waitUntilCatching(
          condition = "Wait for suspended assertion",
          timeout = 100.milliseconds,
          delay = 10.milliseconds,
        ) {
          if (counter++ == 0) {
            throw assertionError
          }

          delay(5.seconds)
        }
      }
      .isSameInstanceAs(assertionError)
  }

  @Test
  fun `waitUntilCatching does not swallow coroutine cancellation`() {
    val cancellation = CancellationException("Test cancellation")
    var counter = 0

    assertFailure {
      waitUntilCatching(condition = "Wait for canceled condition", delay = 10.milliseconds) {
        if (counter++ == 0) {
          error("Previous failure")
        }

        throw cancellation
      }
    }
      .all {
        isInstanceOf<CancellationException>()
        messageContains("Test cancellation")
      }
  }

  @Test
  fun `waitUntilCatching preserves cancellation after a previous assertion failure`() {
    val cancellation = CancellationException("Test cancellation")
    var counter = 0

    assertFailure {
      waitUntilCatching(condition = "Wait for canceled assertion", delay = 10.milliseconds) {
        if (counter++ == 0) {
          throw AssertionError("Previous assertion")
        }

        throw cancellation
      }
    }
      .all {
        isInstanceOf<CancellationException>()
        messageContains("Test cancellation")
      }
  }

  @Test
  fun `waitFor returns the result when the value is non-null within the timeout`() {
    var counter = 0

    val result =
      waitFor(condition = "Wait for result", timeout = 2.seconds, delay = 20.milliseconds) {
        counter++.takeIf { it == 3 }
      }

    assertThat(result).isEqualTo(3)
  }

  @Test
  fun `waitFor returns a value produced by a suspending callback`() {
    var counter = 0

    val result =
      waitFor(condition = "Wait for suspending result", delay = 10.milliseconds) {
        delay(10.milliseconds)
        counter++.takeIf { it == 3 }
      }

    assertThat(result).isEqualTo(3)
  }

  @Test
  fun `waitFor throws an error when the result is null`() {
    assertFailure {
        waitFor<Int>(
          condition = "Wait for result",
          timeout = 100.milliseconds,
          delay = 20.milliseconds,
        ) {
          null
        }
      }
      .messageContains("Waiting for 'Wait for result' never succeeded and the value is null.")
  }

  @Test
  fun `waitFor times out while producing a result`() {
    val elapsed = measureTime {
      assertFailure {
          waitFor<String>(
            condition = "Wait for suspended result",
            timeout = 100.milliseconds,
            delay = 10.milliseconds,
          ) {
            delay(5.seconds)
            "result"
          }
        }
        .messageContains(
          "Waiting for 'Wait for suspended result' never succeeded and the value is null."
        )
    }

    assertThat(elapsed).isLessThan(1.seconds)
  }

  @Test
  fun `waitFor does not swallow coroutine cancellation`() {
    val cancellation = CancellationException("Test cancellation")

    assertFailure {
      waitFor<String>(condition = "Wait for canceled result") { throw cancellation }
    }
      .all {
        isInstanceOf<CancellationException>()
        messageContains("Test cancellation")
      }
  }
}
