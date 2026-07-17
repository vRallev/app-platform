package software.ralf.app.platform.template.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.template.navigation.NavigationHeaderPresenter.Model

@ContributesRenderer
class NavigationHeaderRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Stairs,
            contentDescription = "Icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            "Template App",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
          )
        }

        Text(
          text = "Click Me (times clicked: ${model.clickedCount})",
          color = MaterialTheme.colorScheme.onBackground,
          style = MaterialTheme.typography.titleMedium,
          // Sends event to NavigationHeaderPresenter to be processed which will update
          // the above clickedCount value.
          modifier = Modifier.clickable { model.onEvent(NavigationHeaderPresenter.Event.Clicked) },
        )
      }

      Spacer(
        modifier =
          Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary)
      )
    }
  }
}
