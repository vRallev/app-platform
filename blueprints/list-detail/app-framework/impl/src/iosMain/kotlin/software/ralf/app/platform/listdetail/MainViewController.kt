package software.ralf.app.platform.listdetail

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import software.ralf.app.platform.renderer.ComposeRendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/** Called from Swift to create the Compose Multiplatform view controller. */
@Suppress("unused")
fun mainViewController(rootScopeProvider: RootScopeProvider): UIViewController =
  ComposeUIViewController {
    val templateProvider = remember {
      rootScopeProvider.rootScope
        .metroDependencyGraph<IosMainViewControllerGraph>()
        .templateProviderFactory
        .createTemplateProvider()
    }

    DisposableEffect(templateProvider) { onDispose { templateProvider.cancel() } }

    val rendererFactory = remember { ComposeRendererFactory(rootScopeProvider) }
    val template by templateProvider.templates.collectAsState()

    rendererFactory.getComposeRenderer(template).renderCompose(template)
  }
