package software.ralf.app.platform.template

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import software.ralf.app.platform.presenter.compose.ComposePresenterScope
import software.ralf.app.platform.presenter.compose.ComposePresenterScopeFactory
import software.ralf.app.platform.presenter.compose.launchComposePresenter
import software.ralf.app.platform.template.navigation.NavigationPresenter
import software.ralf.app.platform.template.templates.AppTemplate
import software.ralf.app.platform.template.templates.AppTemplatePresenter

/**
 * Shared class between all platforms to start collecting [AppTemplate] in a [StateFlow]. Inject
 * [Factory] to create a new instance. Once the instance is no longer needed, call [cancel] to clean
 * up any resources.
 *
 * [NavigationPresenter] serves as the root presenter and gets wrapped in a [AppTemplatePresenter].
 */
@AssistedInject
class TemplateProvider(
  presenter: NavigationPresenter,
  templatePresenterFactory: AppTemplatePresenter.Factory,
  @Assisted private val composePresenterScope: ComposePresenterScope,
) {
  /** The templates that should be rendered in the UI. */
  val templates: StateFlow<AppTemplate> by lazy {
    composePresenterScope
      .launchComposePresenter(
        presenter = templatePresenterFactory.createAppTemplatePresenter(presenter),
        input = Unit,
      )
      .model
  }

  /** Releases all resources and stops [templates] from updating further. */
  fun cancel() {
    composePresenterScope.cancel()
  }

  /**
   * The assisted factory for Metro to create a new [TemplateProvider]. This factory is wrapped by
   * [Factory], which should be used instead.
   */
  @AssistedFactory
  fun interface InternalFactory {
    /** Create a new instance of [TemplateProvider] with the given [ComposePresenterScope]. */
    fun create(composePresenterScope: ComposePresenterScope): TemplateProvider
  }

  /** Factory class to create a new instance of [TemplateProvider]. */
  @Inject
  class Factory(
    private val composePresenterScopeFactory: ComposePresenterScopeFactory,
    private val templateProviderFactory: InternalFactory,
  ) {
    /**
     * Creates a new instance of [TemplateProvider]. Call [TemplateProvider.cancel] when the
     * instance not needed anymore to avoid leaking resources.
     */
    fun createTemplateProvider(): TemplateProvider {
      return templateProviderFactory.create(
        composePresenterScopeFactory.createComposePresenterScope()
      )
    }
  }
}
