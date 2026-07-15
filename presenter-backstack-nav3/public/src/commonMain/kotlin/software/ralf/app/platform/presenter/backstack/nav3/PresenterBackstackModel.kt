package software.ralf.app.platform.presenter.backstack.nav3

import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel

/**
 * Model for a presenter-owned backstack rendered by Navigation 3.
 *
 * The presenter remains the owner of navigation state by computing the models in [backstack] and
 * exposing [onBack], while Navigation 3 handles UI back gestures and transitions.
 */
@ExperimentalAppPlatform
public interface PresenterBackstackModel : BaseModel {
  /**
   * Models for every presenter currently in the stack. The last model is the active destination.
   */
  public val backstack: List<BaseModel>

  /** Called when Navigation 3 requests a back navigation. */
  public val onBack: () -> Unit
}
