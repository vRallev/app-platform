package software.ralf.app.platform.sample.user

import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.launch

/**
 * This class logs out the user after a certain delay.
 *
 * The important part of this class is that it implements [Scoped] and it's part of the [UserScope].
 * That means when the user logs in, this class gets automatically instantiated and [onEnterScope]
 * will be called. [onExitScope] will be called on the `CoroutineScope` will be destroyed when the
 * user logs out.
 */
@SingleIn(UserScope::class)
@ContributesScoped(UserScope::class)
class SessionTimeout(private val userManager: UserManager, animationHelper: AnimationHelper) :
  Scoped {

  private val _sessionTimeout = MutableStateFlow(initialTimeout)

  /** The remaining time until the user is logged out. */
  @Suppress("MemberNameEqualsClassName") val sessionTimeout: StateFlow<Duration> = _sessionTimeout

  private val updateDelay =
    if (animationHelper.isAnimationsEnabled()) {
      10.milliseconds
    } else {
      1.seconds
    }

  override fun onEnterScope(scope: Scope) {
    // This job will be automatically canceled when the user logs out and the user scope is
    // destroyed.
    scope.launch {
      while (userManager.user.value != null) {
        delay(updateDelay)

        _sessionTimeout.update { (it - updateDelay).coerceAtLeast(Duration.ZERO) }
      }
    }

    scope.launch {
      sessionTimeout.first { it == Duration.ZERO }
      userManager.logout()
    }
  }

  /** Reset the session timeout to the initial value. */
  fun resetTimeout() {
    _sessionTimeout.value = initialTimeout
  }

  companion object {
    /** The timeout after which the user will be logged out automatically. */
    val initialTimeout = 10.seconds
  }
}
