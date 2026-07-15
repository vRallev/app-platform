@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlin.collections.plus
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter

internal class PresenterBackstackScopeImpl(initial: MoleculePresenter<Unit, out BaseModel>) :
  PresenterBackstackScope {
  private var nextKey = 0
  private val initialEntry = newEntry(initial)
  private var _lastBackstackChange =
    mutableStateOf(
      BackstackChangeImpl(
        entries = listOf(initialEntry),
        action = PresenterBackstackScope.BackstackChange.Action.PUSH,
      )
    )

  override val lastBackstackChange: State<PresenterBackstackScope.BackstackChange> =
    _lastBackstackChange

  val presenterBackstackEntries: List<PresenterBackstackEntry>
    get() = _lastBackstackChange.value.entries

  override fun push(presenter: MoleculePresenter<Unit, out BaseModel>) {
    val oldEntries = _lastBackstackChange.value.entries
    _lastBackstackChange.value =
      BackstackChangeImpl(
        entries = oldEntries + newEntry(presenter),
        action = PresenterBackstackScope.BackstackChange.Action.PUSH,
      )
  }

  override fun pop() {
    val oldEntries = _lastBackstackChange.value.entries
    if (oldEntries.size > 1) {
      _lastBackstackChange.value =
        BackstackChangeImpl(
          entries = oldEntries.subList(0, oldEntries.size - 1),
          action = PresenterBackstackScope.BackstackChange.Action.POP,
        )
    }
  }

  override fun replaceTop(presenter: MoleculePresenter<Unit, out BaseModel>) {
    val oldEntries = _lastBackstackChange.value.entries
    _lastBackstackChange.value =
      BackstackChangeImpl(
        entries = oldEntries.subList(0, oldEntries.size - 1) + newEntry(presenter),
        action = PresenterBackstackScope.BackstackChange.Action.REPLACE,
      )
  }

  private fun newEntry(presenter: MoleculePresenter<Unit, out BaseModel>): PresenterBackstackEntry {
    return PresenterBackstackEntry(key = nextKey++, presenter = presenter)
  }

  private class BackstackChangeImpl(
    val entries: List<PresenterBackstackEntry>,
    override val action: PresenterBackstackScope.BackstackChange.Action,
  ) : PresenterBackstackScope.BackstackChange {
    override val backstack: List<MoleculePresenter<Unit, out BaseModel>> = entries.map {
      it.presenter
    }
  }
}

internal data class PresenterBackstackEntry(
  val key: Int,
  val presenter: MoleculePresenter<Unit, out BaseModel>,
)
