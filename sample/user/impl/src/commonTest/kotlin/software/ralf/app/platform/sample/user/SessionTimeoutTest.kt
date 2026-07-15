package software.ralf.app.platform.sample.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.advanceTimeBy
import software.ralf.app.platform.scope.runTestWithScope

class SessionTimeoutTest {

  @Test
  fun `the timeout reaches zero`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    assertThat(sessionTimeout.sessionTimeout.value).isEqualTo(SessionTimeout.initialTimeout)

    scope.register(sessionTimeout)

    advanceTimeBy(2.seconds + 1.milliseconds)
    assertThat(sessionTimeout.sessionTimeout.value)
      .isEqualTo(SessionTimeout.initialTimeout - 2.seconds)

    // Note that it doesn't go negative.
    advanceTimeBy(SessionTimeout.initialTimeout)
    assertThat(sessionTimeout.sessionTimeout.value).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `on timeout the user is logged out`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    scope.register(sessionTimeout)

    assertThat(userManager.user.value).isNotNull()

    advanceTimeBy(SessionTimeout.initialTimeout + 1.milliseconds)
    assertThat(userManager.user.value).isNull()
  }

  @Test
  fun `the timeout can be reset`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    scope.register(sessionTimeout)

    advanceTimeBy(2.seconds + 1.milliseconds)
    assertThat(sessionTimeout.sessionTimeout.value)
      .isEqualTo(SessionTimeout.initialTimeout - 2.seconds)

    sessionTimeout.resetTimeout()
    assertThat(sessionTimeout.sessionTimeout.value).isEqualTo(SessionTimeout.initialTimeout)
  }

  @Test
  fun `the timeout stops on early logout`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    scope.register(sessionTimeout)

    advanceTimeBy(2.seconds + 1.milliseconds)
    assertThat(sessionTimeout.sessionTimeout.value)
      .isEqualTo(SessionTimeout.initialTimeout - 2.seconds)

    userManager.logout()

    // The timeout doesn't change.
    advanceTimeBy(SessionTimeout.initialTimeout)
    assertThat(sessionTimeout.sessionTimeout.value).isGreaterThan(Duration.ZERO)
  }
}
