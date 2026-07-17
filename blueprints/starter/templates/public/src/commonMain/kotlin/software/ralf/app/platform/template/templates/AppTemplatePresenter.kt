package software.ralf.app.platform.template.templates

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.ralf.app.platform.presenter.template.ModelDelegate
import software.ralf.app.platform.presenter.template.toTemplate

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [AppTemplate]s.
 *
 * Inject [Factory] to create a new instance of [AppTemplatePresenter].
 */
@AssistedInject
class AppTemplatePresenter(@Assisted private val rootPresenter: MoleculePresenter<Unit, *>) :
  MoleculePresenter<Unit, AppTemplate> {
  @Composable
  override fun present(input: Unit): AppTemplate {
    @Suppress("RemoveEmptyParenthesesFromLambdaCall")
    return returningCompositionLocalProvider(
      // Add local composition providers if needed.
    ) {
      rootPresenter.present(Unit).toTemplate<AppTemplate> { AppTemplate.FullScreenTemplate(it) }
    }
  }

  /** A factory to instantiate a new [AppTemplatePresenter] instance. */
  @AssistedFactory
  fun interface Factory {
    /**
     * Create a new [AppTemplatePresenter]. The given [presenter] will be wrapped and its models are
     * transformed into a [AppTemplate] with [AppTemplate.FullScreenTemplate] as default. The given
     * [presenter] can override the template by either returning [AppTemplate] directly or making
     * its [BaseModel] type implement [ModelDelegate].
     */
    fun createAppTemplatePresenter(rootPresenter: MoleculePresenter<Unit, *>): AppTemplatePresenter
  }
}
