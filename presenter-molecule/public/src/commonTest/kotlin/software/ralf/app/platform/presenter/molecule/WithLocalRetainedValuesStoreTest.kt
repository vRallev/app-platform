package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.retain.retainManagedRetainedValuesStore
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel

@OptIn(ExperimentalAppPlatform::class)
class WithLocalRetainedValuesStoreTest {
  @Test
  fun `active child recomposition updates the returned model`() = runTest {
    val count = MutableStateFlow(0)
    val presenter = RecompositionParentPresenter(count)

    presenter.test(this, Unit) {
      assertThat(awaitItem()).isInstanceOf<Model.Child>().prop(Model.Child::count).isEqualTo(0)

      count.value = 1
      assertThat(awaitItem()).isInstanceOf<Model.Child>().prop(Model.Child::count).isEqualTo(1)
    }
  }

  @Test
  fun `retained child state returns after the child leaves composition`() = runTest {
    val showChild = MutableStateFlow(true)
    val presenter = ParentPresenter(ChildPresenter())

    presenter.test(this, showChild) {
      assertThat(awaitItem()).isInstanceOf<Model.Child>().given { firstModel ->
        assertThat(firstModel.count).isEqualTo(0)
        firstModel.onIncrement()
      }
      showChild.value = false
      assertThat(awaitItem()).isEqualTo(Model.Hidden)

      showChild.value = true
      assertThat(awaitItem()).isInstanceOf<Model.Child>().prop(Model.Child::count).isEqualTo(1)
    }
  }

  private class RecompositionParentPresenter(private val count: StateFlow<Int>) :
    MoleculePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      val childRetainedValuesStore = key("child") { retainManagedRetainedValuesStore() }
      return withLocalRetainedValuesStore(childRetainedValuesStore) {
        val currentCount by count.collectAsState()
        Model.Child(count = currentCount, onIncrement = {})
      }
    }
  }

  private class ParentPresenter(private val childPresenter: ChildPresenter) :
    MoleculePresenter<Boolean, Model> {
    @Composable
    override fun present(input: Boolean): Model {
      val childRetainedValuesStore = key("child") { retainManagedRetainedValuesStore() }
      return if (input) {
        withLocalRetainedValuesStore(childRetainedValuesStore) {
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
