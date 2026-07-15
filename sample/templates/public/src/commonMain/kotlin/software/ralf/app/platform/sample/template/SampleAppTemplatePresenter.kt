package software.ralf.app.platform.sample.template

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.ralf.app.platform.presenter.template.ModelDelegate
import software.ralf.app.platform.presenter.template.toTemplate

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [SampleAppTemplate]s.
 *
 * Inject [Factory] to create a new instance of [SampleAppTemplatePresenter].
 */
@AssistedInject
class SampleAppTemplatePresenter(
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
  @Assisted private val rootPresenter: MoleculePresenter<Unit, *>,
) : MoleculePresenter<Unit, SampleAppTemplate> {
  @Composable
  override fun present(input: Unit): SampleAppTemplate {
    return returningCompositionLocalProvider(
      LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
    ) {
      rootPresenter.present(Unit).toTemplate<SampleAppTemplate> {
        SampleAppTemplate.FullScreenTemplate(it)
      }
    }
  }

  /** A factory to instantiate a new [SampleAppTemplatePresenter] instance. */
  @AssistedFactory
  fun interface Factory {
    /**
     * Create a new [SampleAppTemplatePresenter]. The given [presenter] will be wrapped and its
     * models are transformed into a [SampleAppTemplate] with [SampleAppTemplate.FullScreenTemplate]
     * as default. The given [presenter] can override the template by either returning
     * [SampleAppTemplate] directly or making its [BaseModel] type implement [ModelDelegate].
     */
    fun createSampleAppTemplatePresenter(
      rootPresenter: MoleculePresenter<Unit, *>
    ): SampleAppTemplatePresenter
  }
}
