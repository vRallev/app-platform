@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.swiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.recipes.swiftui.SwiftUiChildPresenter.Model

class SwiftUiChildPresenter(
  private val index: Int,
  private val backstack: SnapshotStateList<MoleculePresenter<Unit, out BaseModel>>,
) : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val counter by
      produceState(0) {
        while (isActive) {
          delay(1.seconds)
          value += 1
        }
      }

    return Model(index = index, counter = counter) {
      when (it) {
        Event.AddPeer ->
          backstack.add(SwiftUiChildPresenter(index = index + 1, backstack = backstack))
      }
    }
  }

  data class Model(val index: Int, val counter: Int, val onEvent: (Event) -> Unit) : BaseModel

  sealed interface Event {
    data object AddPeer : Event
  }
}
