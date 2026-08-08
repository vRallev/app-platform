package software.ralf.app.platform.sample

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
 * Responsible for creating the ap [graph] and producing templates. Call [destroy] to clean up any
 * resources.
 *
 * This class is reused in UI tests, but the tests use a different test specific [AppGraph].
 */
class DesktopApp(private val graph: (RootScopeProvider) -> AppGraph) : RootScopeProvider {

  override val rootScope: Scope
    get() = demoApplication.rootScope

  private val demoApplication = DemoApplication().apply { create(graph(this)) }

  private val templateProvider =
    rootScope.metroDependencyGraph<Graph>().templateProviderFactory.createTemplateProvider()

  /** Call this composable function to start rendering templates on the screen. */
  @Composable
  fun renderTemplates() {
    val template by templateProvider.templates.collectAsState()

    val factory = remember { ComposeRendererFactory(demoApplication) }

    val renderer = factory.getComposeRenderer(template)
    renderer.renderCompose(template)
  }

  /** Cancels and releases all resources. */
  fun destroy() {
    templateProvider.cancel()
    demoApplication.destroy()
  }

  /** Graph interface to give us access to objects from the app graph. */
  @ContributesTo(AppScope::class)
  interface Graph {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
