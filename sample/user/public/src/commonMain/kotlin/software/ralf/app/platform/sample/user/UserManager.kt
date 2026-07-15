package software.ralf.app.platform.sample.user

import kotlinx.coroutines.flow.StateFlow

/**
 * Handles login and logout of user and manages the logged in state. Only a single [User] can login
 * at a time.
 */
interface UserManager {

  /** The currently logged in user or `null`. */
  val user: StateFlow<User?>

  /**
   * Logs in a new user with the given [userId]. Any existing logged in user is logged out first.
   * [user] will be updated before the function returns.
   */
  fun login(userId: Long)

  /** Logs out the current user and resets [user] to `null`. */
  fun logout()
}
