package software.ralf.app.platform.listdetail.templates

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.listdetail.screen.DefaultScreenSizeProvider
import software.ralf.app.platform.listdetail.screen.LocalScreenSize
import software.ralf.app.platform.listdetail.screen.ScreenSize
import software.ralf.app.platform.listdetail.theme.AppTheme
import software.ralf.app.platform.listdetail.theme.ListDetailTheme
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresenters
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

/**
 * Compose renderer for the application's outer template layer.
 *
 * It owns theme installation, safe-area padding, window-size reporting, renderer delegation, and
 * forwarding platform back events to active presenters.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Inject
@ContributesRenderer
class ComposeAppTemplateRenderer(
  private val rendererFactory: RendererFactory,
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
  private val screenSizeProvider: DefaultScreenSizeProvider,
) : ComposeRenderer<AppTemplate>() {
  @Composable
  override fun Compose(model: AppTemplate, modifier: Modifier) {
    val screenSize by screenSizeProvider.screenSize.collectAsState()
    ReportScreenSize()

    CompositionLocalProvider(LocalScreenSize provides screenSize) {
      ListDetailTheme {
        Surface(
          modifier = modifier.fillMaxSize(),
          color = AppTheme.colorScheme.background,
          contentColor = AppTheme.colorScheme.onBackground,
        ) {
          SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
              AppTemplateContent(model)
              backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()
            }
          }
        }
      }
    }
  }

  @Composable
  private fun ReportScreenSize() {
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo) {
      snapshotFlow { windowInfo.containerDpSize }
        .filter { it.isSpecified }
        .map { ScreenSize.from(width = it.width, height = it.height) }
        .distinctUntilChanged()
        .collect { screenSizeProvider.update(it) }
    }
  }

  @Composable
  private fun AppTemplateContent(template: AppTemplate) {
    when (template) {
      is AppTemplate.FullScreenTemplate -> {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          rendererFactory
            .getComposeRenderer(template.model)
            .renderCompose(template.model, Modifier.fillMaxSize())
        }
      }
    }
  }
}
