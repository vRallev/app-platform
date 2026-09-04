package software.ralf.app.platform.listdetail.templates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter

/**
 * Adapts a root feature presenter into the application template stream.
 *
 * It also installs the shared back-gesture dispatcher so nested navigation presenters can handle
 * platform back events without depending on a platform-specific entrypoint.
 */
@AssistedInject
class AppTemplatePresenter(
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
  @Assisted private val rootPresenter: MoleculePresenter<Unit, *>,
) : MoleculePresenter<Unit, AppTemplate> {
  @Composable
  override fun present(input: Unit): AppTemplate {
    return withCompositionLocal(
      LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
    ) {
      rootPresenter.present(Unit).toAppTemplate()
    }
  }

  /** Creates a template presenter around an assisted root feature presenter. */
  @AssistedFactory
  interface Factory {
    /** Creates the application template presenter for [rootPresenter]. */
    fun createAppTemplatePresenter(rootPresenter: MoleculePresenter<Unit, *>): AppTemplatePresenter
  }
}
