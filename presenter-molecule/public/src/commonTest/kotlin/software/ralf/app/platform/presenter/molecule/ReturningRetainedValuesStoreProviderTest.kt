package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel

@OptIn(ExperimentalAppPlatform::class)
class ReturningRetainedValuesStoreProviderTest {
  @Test
  fun `retained child state returns after the child leaves composition`() = runTest {
    val showChild = MutableStateFlow(true)
    val presenter = ParentPresenter(ChildPresenter())

    presenter.test(this, showChild) {
      val firstModel = awaitItem() as Model.Child
      assertThat(firstModel.count).isEqualTo(0)

      firstModel.onIncrement()
      showChild.value = false
      assertThat(awaitItem()).isEqualTo(Model.Hidden)

      showChild.value = true
      val restoredModel = awaitItem() as Model.Child
      assertThat(restoredModel.count).isEqualTo(1)
    }
  }

  private class ParentPresenter(private val childPresenter: ChildPresenter) :
    MoleculePresenter<Boolean, Model> {
    @Composable
    override fun present(input: Boolean): Model {
      val retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
      return if (input) {
        retainedValuesStoreRegistry.returningRetainedValuesStoreProvider("child") {
          childPresenter.present(Unit)
        }
      } else {
        Model.Hidden
      }
    }
  }

  private class ChildPresenter : MoleculePresenter<Unit, Model.Child> {
    @Composable
    override fun present(input: Unit): Model.Child {
      val counter = retain { Counter() }
      return Model.Child(count = counter.count, onIncrement = { counter.count++ })
    }
  }

  private class Counter(var count: Int = 0)

  private sealed interface Model : BaseModel {
    data object Hidden : Model

    data class Child(val count: Int, val onIncrement: () -> Unit) : Model
  }
}
