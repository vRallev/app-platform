package software.ralf.app.platform.sample.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.sample.user.UserPageListPresenter.Model

/** Renders the content for [UserPageListPresenter] on screen using Compose Multiplatform. */
@ContributesRenderer
class UserPageListRenderer : ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
      Text(
        text = "User: ${model.userId}",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h5,
        modifier = Modifier.padding(8.dp).testTag("userIdText"),
      )

      LazyColumn {
        itemsIndexed(model.attributeKeys) { index, attribute ->
          Text(
            text = attribute,
            modifier =
              Modifier.fillMaxWidth()
                .clickable(
                  onClick = { model.onEvent(UserPageListPresenter.Event.ItemSelected(index)) },
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(),
                )
                .let { modifier ->
                  if (model.selectedIndex == index) {
                    @Suppress("MagicNumber") modifier.background(Color.DarkGray.copy(0.1f))
                  } else {
                    modifier
                  }
                }
                .padding(8.dp),
          )
        }
      }
    }
  }
}
