package software.ralf.app.platform.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.ralf.app.platform.renderer.ComposeRendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.kotlinInjectComponent

/** Owns the Wasm root scope and renders its shared template stream. */
class WasmJsApp(private val component: (RootScopeProvider) -> AppComponent) : RootScopeProvider {

  override val rootScope: Scope
    get() = demoApplication.rootScope

  private val demoApplication = DemoApplication().apply { create(component(this)) }

  private val templateProvider =
    rootScope.kotlinInjectComponent<Component>().templateProviderFactory.createTemplateProvider()

  /** Renders the latest template. */
  @Composable
  fun renderTemplates() {
    val template by templateProvider.templates.collectAsState()
    val rendererFactory = remember { ComposeRendererFactory(demoApplication) }

    rendererFactory.getComposeRenderer(template).renderCompose(template)
  }

  /** Releases presenter and application resources. */
  fun destroy() {
    templateProvider.cancel()
    demoApplication.destroy()
  }

  /** Component access needed by the Wasm runtime. */
  @ContributesTo(AppScope::class)
  interface Component {
    /** Factory used to create the browser's independently cancellable template stream. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
