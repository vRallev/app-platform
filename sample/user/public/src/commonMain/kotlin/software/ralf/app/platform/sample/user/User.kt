package software.ralf.app.platform.sample.user

import software.ralf.app.platform.scope.Scope

/** A [User] represents an account to implement login and logout in our sample app. */
interface User {
  /** A unique ID of this user. */
  val userId: Long

  /** User specific attributes just to show some data in the sample app. */
  val attributes: List<Attribute>

  /**
   * The scope is tied to the lifecycle of the user. It hosts a user specific `CoroutineScope` and
   * Metro graph. The scope is destroyed on logout.
   */
  val scope: Scope

  /**
   * A [key] [value] pair of user specific data. [metadata] is true when the element should not be
   * shown on screen.
   */
  data class Attribute(val key: String, val value: String, val metadata: Boolean = false) {
    companion object {
      /** Key for the picture attribute. */
      const val PICTURE_KEY = "picture"
    }
  }
}
