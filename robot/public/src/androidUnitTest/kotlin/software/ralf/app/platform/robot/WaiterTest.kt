package software.ralf.app.platform.robot

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.cause
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
  fun `waitFor returns the result when the value is non-null within the timeout`() {
    var counter = 0

    val result =
      waitFor(condition = "Wait for result", timeout = 2.seconds, delay = 20.milliseconds) {
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
}
