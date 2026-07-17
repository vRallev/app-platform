package software.ralf.app.platform.template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.template.navigation.NavigationHeaderPresenter.Model

@Inject
@ContributesBinding(AppScope::class)
class NavigationHeaderPresenterImpl() : NavigationHeaderPresenter {
  @Composable
  override fun present(input: Unit): Model {
    var clickedCount by remember { mutableStateOf(0) }

    return Model(clickedCount = clickedCount) {
      when (it) {
        NavigationHeaderPresenter.Event.Clicked -> {
          clickedCount++
        }
      }
    }
  }
}
