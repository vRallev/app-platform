package software.ralf.app.platform.listdetail

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

/** Owns the Desktop root scope and renders its shared template stream. */
class DesktopApp(private val graph: (RootScopeProvider) -> AppGraph) : RootScopeProvider {
  private val application = Application().apply { create(graph(this)) }
  private val templateProvider =
    application.rootScope
      .metroDependencyGraph<Graph>()
      .templateProviderFactory
      .createTemplateProvider()

  override val rootScope: Scope
    get() = application.rootScope

  /** Renders the latest template. */
  @Composable
  fun renderTemplates() {
    val template by templateProvider.templates.collectAsState()
    val rendererFactory = remember { ComposeRendererFactory(application) }

    rendererFactory.getComposeRenderer(template).renderCompose(template)
  }

  /** Releases presenter and application resources. */
  fun destroy() {
    templateProvider.cancel()
    application.destroy()
  }

  /** Graph access needed by the Desktop runtime. */
  @ContributesTo(AppScope::class)
  interface Graph {
    /** Factory used to create the window's independently cancellable template stream. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
