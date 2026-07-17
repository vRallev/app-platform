package software.ralf.app.platform.template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import software.ralf.app.platform.template.navigation.NavigationDetailPresenter.Model

@Inject
@ContributesBinding(AppScope::class)
class NavigationDetailPresenterImpl(private val exampleRepository: ExampleRepository) :
  NavigationDetailPresenter {
  @Composable
  override fun present(input: Unit): Model {
    val exampleValue by exampleRepository.exampleStateFlow.collectAsState()
    var exampleCount by remember { mutableStateOf(0) }

    LaunchedEffect(exampleValue) {
      // Add a delay, otherwise the state is not updating properly on iOS.
      delay(1.milliseconds)
      exampleCount++
    }

    return Model(exampleValue = exampleValue, exampleCount = exampleCount)
  }
}
