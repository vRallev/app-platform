package software.ralf.app.platform.sample.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.sample.template.animation.AnimationContentKey
import software.ralf.app.platform.sample.user.UserPageDetailPresenter.Input
import software.ralf.app.platform.sample.user.UserPageDetailPresenter.Model

/** Presenter to manage the detail content of the list-detail layout. */
@Inject
class UserPageDetailPresenter(private val sessionTimeout: SessionTimeout) :
  ComposePresenter<Input, Model> {

  @Composable
  override fun present(input: Input): Model {
    var showPictureFullscreen by remember { mutableStateOf(false) }

    val timeout = sessionTimeout.sessionTimeout.collectAsState()
    val timeoutProgress = remember {
      derivedStateOf { getSessionTimeoutProgress(timeout.value) }
    }

    return Model(
      text = input.user.attributes[input.selectedAttribute].value,
      pictureKey = input.user.attributes.single { it.key == User.Attribute.PICTURE_KEY }.value,
      timeoutProgress = timeoutProgress,
      showPictureFullscreen = showPictureFullscreen,
    ) {
      when (it) {
        Event.ProfilePictureClick -> {
          showPictureFullscreen = !showPictureFullscreen
        }
      }
    }
  }

  private fun getSessionTimeoutProgress(timeout: Duration): Float {
    return (timeout / SessionTimeout.initialTimeout).toFloat()
  }

  /** The state of the detail pane. */
  data class Model(
    /** The text rendered on screen. Usually, refers to the selected user attribute. */
    val text: String,
    /** The profile picture ID loaded from the resources. */
    val pictureKey: String,
    /**
     * The progress until when current user is logged out. The value is between [0, 1].
     *
     * Note that this property is a composable [State]. Updates of this value are not propagated
     * through a new [Model] and consumers must observe the value directly instead. In a Compose UI
     * layer this is trivial, because the Compose runtime does this automatically.
     *
     * Using this approach for the timeout is much more efficient, because the value changes every
     * 10 milliseconds and going through the whole presenter tree to compute a new model and
     * updating all renderers adds a lot of load. With this approach only the necessary Composable
     * UI element is updated.
     */
    val timeoutProgress: State<Float>,
    /** If this value is true, then the profile picture is shown in full screen. */
    val showPictureFullscreen: Boolean,
    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit,
  ) : BaseModel, AnimationContentKey {
    override val contentKey: Int =
      if (showPictureFullscreen) 1 else AnimationContentKey.DEFAULT_CONTENT_KEY
  }

  /** All events that [UserPageDetailPresenter] can process. */
  sealed interface Event {
    /** Sent when the user taps on the profile picture. */
    data object ProfilePictureClick : Event
  }

  /**
   * The input type of the presenter. [user] is the currently logged in user. [selectedAttribute] is
   * the index of the selected attribute in the UI.
   */
  data class Input(val user: User, val selectedAttribute: Int)
}
