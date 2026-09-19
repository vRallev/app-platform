package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.listdetail.approot.AppRootPresenter
import software.ralf.app.platform.listdetail.templates.AppTemplate
import software.ralf.app.platform.listdetail.templates.AppTemplatePresenter
import software.ralf.app.platform.presenter.compose.ComposePresenterScope
import software.ralf.app.platform.presenter.compose.ComposePresenterScopeFactory
import software.ralf.app.platform.presenter.compose.launchComposePresenter

/** Produces the application template stream for a platform renderer. */
@AssistedInject
class TemplateProvider(
  presenter: AppRootPresenter,
  templatePresenterFactory: AppTemplatePresenter.Factory,
  @Assisted private val composePresenterScope: ComposePresenterScope,
) {
  /** Templates emitted by the root presenter. */
  val templates: StateFlow<AppTemplate> by lazy {
    composePresenterScope
      .launchComposePresenter(
        presenter = templatePresenterFactory.createAppTemplatePresenter(presenter),
        input = Unit,
      )
      .model
  }

  /** Stops template production and releases presenter resources. */
  fun cancel() {
    composePresenterScope.cancel()
  }

  /** Metro-assisted factory that accepts the platform-owned Compose presenter scope. */
  @AssistedFactory
  interface InternalFactory {
    /** Creates a provider whose work is owned by [composePresenterScope]. */
    fun create(composePresenterScope: ComposePresenterScope): TemplateProvider
  }

  /** Public factory that creates each provider with its own Compose presenter scope. */
  @Inject
  class Factory(
    private val composePresenterScopeFactory: ComposePresenterScopeFactory,
    private val templateProviderFactory: InternalFactory,
  ) {
    /** Creates a provider with a fresh Compose presenter scope that it can cancel independently. */
    fun createTemplateProvider(): TemplateProvider {
      return templateProviderFactory.create(
        composePresenterScopeFactory.createComposePresenterScope()
      )
    }
  }
}
