@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.appbar.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.recipes.appbar.AppBarConfig
import software.ralf.app.platform.recipes.appbar.AppBarConfigModel
import software.ralf.app.platform.recipes.appbar.menu.MenuPresenter.Model
import software.ralf.app.platform.renderer.ComposeRenderer

/** This presenter provides a custom menu in the App Bar. */
class MenuPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    var itemCount by remember { mutableIntStateOf(2) }
    var pressedItem by remember { mutableStateOf<Int?>(null) }

    val items =
      List(itemCount) {
        val number = it + 1
        AppBarConfig.MenuItem(text = "Option $number", action = { pressedItem = number })
      }

    LaunchedEffect(pressedItem) {
      delay(3.seconds)
      pressedItem = null
    }

    return Model(items, pressedItem) {
      when (it) {
        Event.AddMenuItem -> itemCount += 1
      }
    }
  }

  data class Model(
    private val menuItems: List<AppBarConfig.MenuItem>,
    val pressedItem: Int?,
    val onEvent: (Event) -> Unit,
  ) : BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      return AppBarConfig(title = "Menu items", menuItems = menuItems)
    }
  }

  sealed interface Event {
    data object AddMenuItem : Event
  }
}

@ContributesRenderer
class MenuRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(onClick = { model.onEvent(MenuPresenter.Event.AddMenuItem) }) { Text("Add menu item") }

      AnimatedContent(targetState = model, contentKey = { it.pressedItem != null }) { targetModel ->
        if (targetModel.pressedItem != null) {
          Text(
            text = "Pressed option ${targetModel.pressedItem}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
          )
        }
      }
    }
  }
}
