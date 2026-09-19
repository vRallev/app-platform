@file:OptIn(ExperimentalAppPlatform::class)
@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.backstack.presenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.LocalBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.requireNotNull
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.recipes.appbar.AppBarConfig
import software.ralf.app.platform.recipes.appbar.AppBarConfigModel
import software.ralf.app.platform.recipes.backstack.presenter.BackstackChildPresenter.Model
import software.ralf.app.platform.renderer.ComposeRenderer

/**
 * A presenter that is added to the backstack and has a button to put a new instance on top of the
 * stack.
 */
class BackstackChildPresenter(private val index: Int) : ComposePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = LocalBackstackScope.requireNotNull()

    var counter by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
      while (isActive) {
        delay(1.seconds)
        counter += 1
      }
    }

    return Model(index = index, counter = counter) {
      when (it) {
        Event.AddPresenterToBackstack -> backstack.push(BackstackChildPresenter(index = index + 1))
      }
    }
  }

  data class Model(val index: Int, val counter: Int, val onEvent: (Event) -> Unit) :
    BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      return AppBarConfig(title = "Backstack")
    }
  }

  sealed interface Event {
    data object AddPresenterToBackstack : Event
  }
}

@ContributesRenderer
class BackstackChildRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model, modifier: Modifier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text("Index: ${model.index}")
      Text("Counter: ${model.counter}")

      Button(onClick = { model.onEvent(BackstackChildPresenter.Event.AddPresenterToBackstack) }) {
        Text("Add presenter to backstack")
      }
    }
  }
}
