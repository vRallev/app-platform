package software.ralf.app.platform.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.createGraphFactory
import kotlinx.browser.document
import software.ralf.app.platform.renderer.ComposeRendererFactory
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/** The entry point of our sample app. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  ComposeViewport(checkNotNull(document.body)) { AppPlatform() }
}

@Composable
private fun AppPlatform() {
  val application = remember {
    Application().apply { create(createGraphFactory<WasmJsAppGraph.Factory>().create(this)) }
  }

  // Create a single instance.
  val templateProvider = remember {
    application.rootScope
      .metroDependencyGraph<WasmJsAppGraph>()
      .templateProviderFactory
      .createTemplateProvider()
  }

  DisposableEffect(Unit) {
    onDispose {
      // Cancel the provider when it's no longer needed.
      templateProvider.cancel()
    }
  }

  // Only a single factory is needed.
  val factory = remember { ComposeRendererFactory(application) }

  // Render templates using our Renderer runtime.
  val template by templateProvider.templates.collectAsState()

  val renderer = factory.getRenderer(template::class)
  renderer.renderCompose(template)
}
