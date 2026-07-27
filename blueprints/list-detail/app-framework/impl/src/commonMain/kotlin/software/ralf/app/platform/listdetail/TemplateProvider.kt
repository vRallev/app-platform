package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.listdetail.approot.AppRootPresenter
import software.ralf.app.platform.listdetail.templates.AppTemplate
import software.ralf.app.platform.listdetail.templates.AppTemplatePresenter
import software.ralf.app.platform.presenter.molecule.MoleculeScope
import software.ralf.app.platform.presenter.molecule.MoleculeScopeFactory
import software.ralf.app.platform.presenter.molecule.launchMoleculePresenter

/** Produces the application template stream for a platform renderer. */
@AssistedInject
class TemplateProvider(
  presenter: AppRootPresenter,
  templatePresenterFactory: AppTemplatePresenter.Factory,
  @Assisted private val moleculeScope: MoleculeScope,
) {
  /** Templates emitted by the root presenter. */
  val templates: StateFlow<AppTemplate> by lazy {
    moleculeScope
      .launchMoleculePresenter(
        presenter = templatePresenterFactory.createAppTemplatePresenter(presenter),
        input = Unit,
      )
      .model
  }

  /** Stops template production and releases presenter resources. */
  fun cancel() {
    moleculeScope.cancel()
  }

  /** Metro-assisted factory that accepts the platform-owned Molecule scope. */
  @AssistedFactory
  interface InternalFactory {
    /** Creates a provider whose work is owned by [moleculeScope]. */
    fun create(moleculeScope: MoleculeScope): TemplateProvider
  }

  /** Public factory that creates each provider with its own Molecule scope. */
  @Inject
  class Factory(
    private val moleculeScopeFactory: MoleculeScopeFactory,
    private val templateProviderFactory: InternalFactory,
  ) {
    /** Creates a provider with a fresh Molecule scope that it can cancel independently. */
    fun createTemplateProvider(): TemplateProvider {
      return templateProviderFactory.create(moleculeScopeFactory.createMoleculeScope())
    }
  }
}
