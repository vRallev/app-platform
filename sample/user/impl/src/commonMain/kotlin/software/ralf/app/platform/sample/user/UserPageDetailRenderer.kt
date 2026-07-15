package software.ralf.app.platform.sample.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app_platform.sample.user.impl.generated.resources.Res
import app_platform.sample.user.impl.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.painterResource
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.sample.template.animation.LocalAnimatedVisibilityScope
import software.ralf.app.platform.sample.template.animation.LocalSharedTransitionScope
import software.ralf.app.platform.sample.user.UserPageDetailPresenter.Model

/** Renders the content for [UserPageDetailPresenter] on screen using Compose Multiplatform. */
@OptIn(ExperimentalSharedTransitionApi::class)
@ContributesRenderer
class UserPageDetailRenderer : ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    if (model.showPictureFullscreen) {
      ProfilePicture(model)
    } else {
      ProfileDetails(model)
    }
  }

  @Composable
  private fun ProfileDetails(model: Model) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
      LinearProgressIndicator(
        progress = model.timeoutProgress.value,
        modifier = Modifier.fillMaxWidth(),
      )

      with(checkNotNull(LocalSharedTransitionScope.current)) {
        Image(
          painter = painterResource(Res.allDrawableResources.getValue(model.pictureKey)),
          contentDescription = "Profile picture",
          modifier =
            Modifier.padding(start = 64.dp, top = 16.dp, end = 64.dp)
              .testTag("profilePicture")
              .sharedElement(
                rememberSharedContentState(key = PROFILE_PICTURE_KEY),
                animatedVisibilityScope = checkNotNull(LocalAnimatedVisibilityScope.current),
                clipInOverlayDuringTransition = OverlayClip(CircleShape),
              )
              .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = MaterialTheme.colors.primary,
                spotColor = MaterialTheme.colors.primary,
              )
              .clip(CircleShape) // clip to the circle shape
              .clickable { model.onEvent(UserPageDetailPresenter.Event.ProfilePictureClick) }
              .border(2.dp, MaterialTheme.colors.primary, shape = CircleShape),
        )

        AnimatedContent(targetState = model.text) { text ->
          Text(
            text = text,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
      }
    }
  }

  @Composable
  private fun ProfilePicture(model: Model) {
    with(checkNotNull(LocalSharedTransitionScope.current)) {
      Row(Modifier.background(Color.Black).fillMaxSize()) {
        Image(
          painter = painterResource(Res.allDrawableResources.getValue(model.pictureKey)),
          contentDescription = "Profile picture",
          modifier =
            Modifier.clickable { model.onEvent(UserPageDetailPresenter.Event.ProfilePictureClick) }
              .testTag("profilePicture")
              .align(Alignment.CenterVertically)
              .sharedElement(
                rememberSharedContentState(key = PROFILE_PICTURE_KEY),
                animatedVisibilityScope = checkNotNull(LocalAnimatedVisibilityScope.current),
                clipInOverlayDuringTransition = OverlayClip(CircleShape),
              )
              .clip(CircleShape),
        )
      }
    }
  }

  private companion object {
    const val PROFILE_PICTURE_KEY = "profile-picture"
  }
}
