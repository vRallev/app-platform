package software.ralf.app.platform.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.ralf.app.platform.recipes.template.RecipesAppTemplate
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.di.kotlinInjectComponent

/**
 * `ViewModel` that hosts the stream of templates and survives configuration changes. Note that we
 * use [application] to get access to the root scope.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val component =
    (application as RootScopeProvider).rootScope.kotlinInjectComponent<Component>()
  private val templateProvider = component.templateProviderFactory.createTemplateProvider()

  /** The stream of templates that are rendered by [MainActivity]. */
  val templates: StateFlow<RecipesAppTemplate> = templateProvider.templates

  override fun onCleared() {
    templateProvider.cancel()
  }

  /** Component interface to give us access to objects from the app component. */
  @ContributesTo(AppScope::class)
  interface Component {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
