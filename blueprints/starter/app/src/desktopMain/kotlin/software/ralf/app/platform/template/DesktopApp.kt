package software.ralf.app.platform.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import software.ralf.app.platform.renderer.ComposeRendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/**
 * Responsible for creating the app graph [graph] and producing templates. Call [destroy] to clean
 * up any resources.
 */
class DesktopApp(private val graph: (RootScopeProvider) -> AppGraph) : RootScopeProvider {
  override val rootScope: Scope
    get() = application.rootScope

  private val application = Application().apply { create(graph(this)) }

  private val templateProvider =
    rootScope.metroDependencyGraph<Graph>().templateProviderFactory.createTemplateProvider()

  /** Call this composable function to start rendering templates on the screen. */
  @Composable
  fun renderTemplates() {
    val template by templateProvider.templates.collectAsState()

    val factory = remember { ComposeRendererFactory(application) }

    val renderer = factory.getComposeRenderer(template)
    renderer.renderCompose(template)
  }

  /** Cancels and releases all resources. */
  fun destroy() {
    templateProvider.cancel()
    application.destroy()
  }

  /** Graph interface to give us access to objects from the app graph. */
  @ContributesTo(AppScope::class)
  interface Graph {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
