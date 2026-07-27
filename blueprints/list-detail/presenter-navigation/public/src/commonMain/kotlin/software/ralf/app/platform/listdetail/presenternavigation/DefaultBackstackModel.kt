@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.listdetail.presenternavigation

import androidx.compose.runtime.Composable
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackModel
import software.ralf.app.platform.presenter.backstack.nav3.presenterBackstack
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

/**
 * Default renderer-facing representation of an App Platform presenter backstack.
 *
 * Feature modules can reuse this model when they only need conventional push and pop behavior.
 */
data class DefaultBackstackModel(
  override val backstack: List<BaseModel>,
  override val onBack: () -> Unit,
) : PresenterBackstackModel

/** Presents [initialPresenter] in a backstack that pops its current entry on back. */
@Composable
fun presenterBackstackDefault(
  initialPresenter: MoleculePresenter<Unit, out BaseModel>
): DefaultBackstackModel {
  return presenterBackstack(initialPresenter) { backstack ->
    DefaultBackstackModel(backstack = backstack, onBack = { pop() })
  }
}
