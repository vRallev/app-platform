package software.ralf.app.platform.template

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import software.ralf.app.platform.renderer.ComposeRendererFactory
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/**
 * This function is called from Swift to hook up the Compose Multiplatform UI.
 *
 * This is our entry point to start producing templates and hooking up our [Renderer] runtime. Other
 * platforms extract this code into classes that are effectively singletons. But this approach is
 * good enough for the iOS sample.
 */
@Suppress("unused")
fun mainViewController(rootScopeProvider: RootScopeProvider): UIViewController =
  ComposeUIViewController {
    // Create a single instance.
    val templateProvider = remember {
      rootScopeProvider.rootScope
        .metroDependencyGraph<IosAppGraph>()
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
    val factory = remember { ComposeRendererFactory(rootScopeProvider) }

    // Render templates using our Renderer runtime.
    val template by templateProvider.templates.collectAsState()

    val renderer = factory.getRenderer(template::class)
    renderer.renderCompose(template)
  }
