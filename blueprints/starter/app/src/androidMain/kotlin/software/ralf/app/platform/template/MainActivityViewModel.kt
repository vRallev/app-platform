package software.ralf.app.platform.template

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph
import software.ralf.app.platform.template.templates.AppTemplate

/**
 * `ViewModel` that hosts the stream of templates and survives configuration changes. Note that we
 * use [application] to get access to the root scope.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
  private val graph = (application as RootScopeProvider).rootScope.metroDependencyGraph<Graph>()
  private val templateProvider = graph.templateProviderFactory.createTemplateProvider()

  /** The stream of templates that are rendered by [MainActivity]. */
  val templates: StateFlow<AppTemplate> = templateProvider.templates

  override fun onCleared() {
    templateProvider.cancel()
  }

  /** Graph interface to give us access to objects from the app graph. */
  @ContributesTo(AppScope::class)
  interface Graph {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
