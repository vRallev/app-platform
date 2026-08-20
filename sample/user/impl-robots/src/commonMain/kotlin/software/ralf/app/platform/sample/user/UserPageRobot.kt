package software.ralf.app.platform.sample.user

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.ComposeRobot

/**
 * A test robot to verify interactions with the user page screen written with Compose Multiplatform.
 *
 * This robot injects the [User] from the user-scoped object graph. Robots are not exclusive to
 * verifying UI and UI interactions and can use dependencies from their contributed scope.
 */
@ContributesRobot(UserScope::class)
class UserPageRobot(private val user: User) : ComposeRobot() {

  private val userIdTextNode
    get() = compose.onNodeWithTag("userIdText")

  private val profilePictureNode
    get() = compose.onNodeWithTag("profilePicture")

  /**
   * Verify that the user ID is displayed. The [userId] can be changed, but defaults to the ID of
   * the user associated with this robot's scope.
   */
  fun seeUserId(userId: Long = user.userId) {
    userIdTextNode.assertIsDisplayed()
    userIdTextNode.assertTextEquals("User: $userId")
  }

  /**
   * Verify that the profile picture is displayed. If [fullScreen] is `true`, then only the picture
   * should be shown and other elements like the user ID [seeUserId] are gone.
   */
  fun seeProfilePicture(fullScreen: Boolean = false) {
    profilePictureNode.assertIsDisplayed()

    if (fullScreen) {
      userIdTextNode.assertDoesNotExist()
    } else {
      userIdTextNode.assertIsDisplayed()
    }
  }

  /** Click on the profile picture. This works in the detail page and fullscreen mode. */
  fun clickProfilePicture() {
    profilePictureNode.performClick()
  }
}
