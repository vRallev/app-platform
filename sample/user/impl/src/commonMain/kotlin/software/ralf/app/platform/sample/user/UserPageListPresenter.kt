package software.ralf.app.platform.sample.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.sample.user.UserPageListPresenter.Input
import software.ralf.app.platform.sample.user.UserPageListPresenter.Model

/** Presenter to manage the list content of the list-detail layout. */
@Inject
class UserPageListPresenter(private val sessionTimeout: SessionTimeout) :
  MoleculePresenter<Input, Model> {

  @Composable
  override fun present(input: Input): Model {
    val user = input.user
    var selectedIndex by remember { mutableIntStateOf(0) }

    return Model(
      userId = user.userId,
      attributeKeys = user.attributes.filterNot { it.metadata }.map { it.key },
      selectedIndex = selectedIndex,
    ) {
      when (it) {
        is Event.ItemSelected -> {
          sessionTimeout.resetTimeout()
          selectedIndex = it.index
        }
      }
    }
  }

  /** The state of the list pane. */
  data class Model(
    /** The ID of the currently logged in user. */
    val userId: Long,
    /** The attributes that can be selected in the in UI. */
    val attributeKeys: List<String>,
    /** The currently selected attribute. */
    val selectedIndex: Int,
    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit,
  ) : BaseModel

  /** All events that [UserPageListPresenter] can process. */
  sealed interface Event {
    /** Sent when the user selects item with [index] in the list. */
    data class ItemSelected(val index: Int) : Event
  }

  /**
   * The input type of the presenter. [user] is the currently logged in user. More parameters can be
   * added to this class when needed.
   */
  data class Input(val user: User)
}
