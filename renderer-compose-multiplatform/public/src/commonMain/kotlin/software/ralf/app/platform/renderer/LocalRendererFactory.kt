package software.ralf.app.platform.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The factory used to render child models in this composition, or `null` outside a renderer tree.
 *
 * Renderers obtained from [ComposeRendererFactory] or `ComposeAndroidRendererFactory` provide it
 * automatically. An existing value takes precedence. Tests and previews can provide their own
 * factory with [CompositionLocalProvider].
 */
public val LocalRendererFactory: ProvidableCompositionLocal<RendererFactory?> =
  staticCompositionLocalOf {
    null
  }

@Composable
internal inline fun ProvideRendererFactory(
  factory: RendererFactory?,
  crossinline content: @Composable () -> Unit,
) {
  if (factory != null && LocalRendererFactory.current == null) {
    CompositionLocalProvider(LocalRendererFactory provides factory) { content() }
  } else {
    content()
  }
}
