package software.ralf.circuit.listdetail.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import software.ralf.circuit.listdetail.screen.DefaultScreenSizeProvider
import software.ralf.circuit.listdetail.screen.LocalScreenSize
import software.ralf.circuit.listdetail.screen.ScreenSize
import software.ralf.circuit.listdetail.theme.AppTheme
import software.ralf.circuit.listdetail.theme.ListDetailTheme

@Composable
fun AppScaffold(
  screenSizeProvider: DefaultScreenSizeProvider,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val screenSize by screenSizeProvider.screenSize.collectAsState()
  val windowInfo = LocalWindowInfo.current
  LaunchedEffect(windowInfo) {
    snapshotFlow { windowInfo.containerDpSize }
      .filter { it.isSpecified }
      .map { ScreenSize.from(width = it.width, height = it.height) }
      .distinctUntilChanged()
      .collect { screenSizeProvider.update(it) }
  }

  CompositionLocalProvider(LocalScreenSize provides screenSize) {
    ListDetailTheme {
      Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colorScheme.background,
        contentColor = AppTheme.colorScheme.onBackground,
      ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          content()
        }
      }
    }
  }
}
