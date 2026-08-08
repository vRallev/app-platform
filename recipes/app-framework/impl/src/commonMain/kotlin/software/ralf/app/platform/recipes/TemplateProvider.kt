package software.ralf.app.platform.recipes

import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.presenter.molecule.MoleculeScope
import software.ralf.app.platform.presenter.molecule.MoleculeScopeFactory
import software.ralf.app.platform.presenter.molecule.launchMoleculePresenter
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
  @Assisted private val moleculeScope: MoleculeScope,
) {

  /** The templates that should be rendered in the UI. */
  val templates: StateFlow<RecipesAppTemplate> by lazy {
    moleculeScope.launchMoleculePresenter(presenter = presenter, input = Unit).model
  }

  /** Releases all resources and stops [templates] from updating further. */
  fun cancel() {
    moleculeScope.cancel()
  }

  /** Factory class to create a new instance of [TemplateProvider]. */
  // Note that the Factory class technically is not required. But since TemplateProvider
  // contains a MoleculeScope that needs to be canceled explicitly, this Factory helps to
  // highlight that the created instance contains resources that must be cleaned up.
  @Inject
  class Factory(
    private val moleculeScopeFactory: MoleculeScopeFactory,
    private val templateProvider: (MoleculeScope) -> TemplateProvider,
  ) {
    /**
     * Creates a new instance of [TemplateProvider]. Call [TemplateProvider.cancel] when the
     * instance not needed anymore to avoid leaking resources.
     */
    fun createTemplateProvider(): TemplateProvider {
      return templateProvider(moleculeScopeFactory.createMoleculeScope())
    }
  }
}
