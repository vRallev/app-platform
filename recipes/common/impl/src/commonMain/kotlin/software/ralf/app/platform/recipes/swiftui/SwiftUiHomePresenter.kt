@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.swiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.recipes.swiftui.SwiftUiHomePresenter.Model

/**
 * A presenter that manages a backstack of presenters that are rendered by SwiftUI's
 * `NavigationStack`. All presenters in this backstack are always active, because `NavigationStack`
 * renders them on stack modification. In SwiftUI this is necessary as views remain alive even when
 * they are no longer visible.
 *
 * A detail of note for this class is that we pass a list of [BaseModel] to the view but receive a
 * list of [Int] back where each integer represents the position of a presenter in the backstack
 * list. This is because to share control of state with `NavigationStack` we need to initialize the
 * `NavigationStack` with a `Binding` to a collection of `Hashable` data values. [BaseModel] by
 * default is not `Hashable` and we cannot extend it to conform to `Hashable` due to current
 * Kotlin-Swift interop limitations. As such in Swift the list of [BaseModel] is converted to a list
 * of indices, which are hashable by default. This should be sufficient to handle most navigation
 * cases but if it is required to receive more information to determine how to modify the presenter
 * backstack, it is possible to create a generic class that implements [BaseModel] and wrap that
 * class in a hashable `struct`.
 */
@Inject
class SwiftUiHomePresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = remember {
      mutableStateListOf<MoleculePresenter<Unit, out BaseModel>>().apply {
        // There must be always one element.
        add(SwiftUiChildPresenter(index = 0, backstack = this))
      }
    }

    return Model(modelBackstack = backstack.map { it.present(Unit) }) {
      when (it) {
        is Event.BackstackModificationEvent -> {
          val updatedBackstack = it.indicesBackstack.map { index -> backstack[index] }

          backstack.clear()
          backstack.addAll(updatedBackstack)
        }
      }
    }
  }

  /**
   * Model that contains all the information needed for SwiftUI to render the backstack.
   * [modelBackstack] contains the backage and [onEvent] exposes an event handling function that can
   * be called by the binding that `NavigationStack` is initialized with.
   */
  data class Model(val modelBackstack: List<BaseModel>, val onEvent: (Event) -> Unit) : BaseModel

  /** All events that [SwiftUiHomePresenter] can process. */
  sealed interface Event {
    /** Sent when `NavigationStack` has modified its stack. */
    data class BackstackModificationEvent(val indicesBackstack: List<Int>) : Event
  }
}
