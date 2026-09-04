package software.ralf.app.platform.recipes

import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.presenter.compose.ComposePresenterScope
import software.ralf.app.platform.presenter.compose.ComposePresenterScopeFactory
import software.ralf.app.platform.presenter.compose.launchComposePresenter
import software.ralf.app.platform.recipes.template.RecipesAppTemplate
import software.ralf.app.platform.recipes.template.RootPresenter

/**
 * Shared class between all platforms to start collecting
 * [software.ralf.app.platform.recipes.template.RecipesAppTemplate] in a [StateFlow]. Inject
 * [Factory] to create a new instance. Once the instance is no longer needed, call [cancel] to clean
 * up any resources.
 */
@Inject
class TemplateProvider(
  presenter: RootPresenter,
  @Assisted private val composePresenterScope: ComposePresenterScope,
) {

  /** The templates that should be rendered in the UI. */
  val templates: StateFlow<RecipesAppTemplate> by lazy {
    composePresenterScope.launchComposePresenter(presenter = presenter, input = Unit).model
  }

  /** Releases all resources and stops [templates] from updating further. */
  fun cancel() {
    composePresenterScope.cancel()
  }

  /** Factory class to create a new instance of [TemplateProvider]. */
  // Note that the Factory class technically is not required. But since TemplateProvider
  // contains a ComposePresenterScope that needs to be canceled explicitly, this Factory helps to
  // highlight that the created instance contains resources that must be cleaned up.
  @Inject
  class Factory(
    private val composePresenterScopeFactory: ComposePresenterScopeFactory,
    private val templateProvider: (ComposePresenterScope) -> TemplateProvider,
  ) {
    /**
     * Creates a new instance of [TemplateProvider]. Call [TemplateProvider.cancel] when the
     * instance not needed anymore to avoid leaking resources.
     */
    fun createTemplateProvider(): TemplateProvider {
      return templateProvider(composePresenterScopeFactory.createComposePresenterScope())
    }
  }
}
