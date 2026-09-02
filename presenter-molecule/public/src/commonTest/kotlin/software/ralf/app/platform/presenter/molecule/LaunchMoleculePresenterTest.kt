package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.internal.IgnoreNative
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.internal.MarkedDispatcher
import software.ralf.app.platform.internal.createMarkedDispatcher
import software.ralf.app.platform.presenter.BaseModel

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAppPlatform::class)
class LaunchMoleculePresenterTest {

  @Test
  @IgnoreNative
  @IgnoreWasm
  fun `the first present call happens inline and the second uses the supplied dispatcher`() =
    runTest {
      val inputFlow = MutableStateFlow("1")
      val markedDispatcher = createMarkedDispatcher()
      try {
        ThreadPresenter(markedDispatcher).test(this, inputFlow, markedDispatcher.dispatcher) {
          val model1 = awaitItem()
          inputFlow.value = "2"
          val model2 = awaitItem()

          assertThat(model1.ranOnMarkedDispatcher).isFalse()
          assertThat(model2.ranOnMarkedDispatcher).isTrue()
        }
      } finally {
        markedDispatcher.close()
      }
    }

  @Test
  fun `the presenter is called and computes a new model whenever the input changes`() = runTest {
    data class Model(val value: String) : BaseModel

    val presenter = MoleculePresenter<Int, Model> { input -> Model(input.toString()) }

    val inputFlow = MutableStateFlow(1)

    presenter.test(this, inputFlow) {
      assertThat(awaitItem().value).isEqualTo("1")

      inputFlow.value = 2
      assertThat(awaitItem().value).isEqualTo("2")

      inputFlow.value = 3
      assertThat(awaitItem().value).isEqualTo("3")
    }
  }

  @Test
  fun `the recomposition mode is available in the presenter hierarchy`() = runTest {
    data class Model(val recompositionMode: RecompositionMode) : BaseModel

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          return Model(LocalRecompositionMode.current)
        }
      }

    presenter.test(this, Unit) {
      assertThat(awaitItem().recompositionMode).isEqualTo(RecompositionMode.Immediate)
    }
  }

  @Test
  fun `presentDetached does not recompose child presenters when the parent recomposes`() = runTest {
    data class ChildModel(val presentCalls: Int) : BaseModel
    data class ParentModel(val input: Int, val childPresentCalls: Int) : BaseModel

    var childPresentCalls = 0

    val childPresenter =
      object : MoleculePresenter<Unit, ChildModel> {
        @Composable
        override fun present(input: Unit): ChildModel {
          childPresentCalls++

          return ChildModel(childPresentCalls)
        }
      }

    val parentPresenter =
      object : MoleculePresenter<Int, ParentModel> {
        @Composable
        override fun present(input: Int): ParentModel {
          val childModel = childPresenter.presentDetached(Unit)

          return ParentModel(input = input, childPresentCalls = childModel.presentCalls)
        }
      }

    val inputFlow = MutableStateFlow(1)

    parentPresenter.test(this, inputFlow) {
      assertThat(awaitItem()).isEqualTo(ParentModel(input = 1, childPresentCalls = 1))
      assertThat(childPresentCalls).isEqualTo(1)

      inputFlow.value = 2
      assertThat(awaitItem()).isEqualTo(ParentModel(input = 2, childPresentCalls = 1))
      runCurrent()
      expectNoEvents()
      assertThat(childPresentCalls).isEqualTo(1)

      inputFlow.value = 3
      assertThat(awaitItem()).isEqualTo(ParentModel(input = 3, childPresentCalls = 1))
      runCurrent()
      expectNoEvents()
      assertThat(childPresentCalls).isEqualTo(1)
    }
  }

  @Test
  fun `presentDetached recomposes child presenters when the detached input changes`() = runTest {
    data class Model(val input: Int, val presentCalls: Int) : BaseModel

    var childPresentCalls = 0

    val childPresenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          childPresentCalls++

          return Model(input = input, presentCalls = childPresentCalls)
        }
      }

    val parentPresenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          return childPresenter.presentDetached(input)
        }
      }

    val inputFlow = MutableStateFlow(1)

    parentPresenter.test(this, inputFlow) {
      assertThat(awaitItem()).isEqualTo(Model(input = 1, presentCalls = 1))
      assertThat(childPresentCalls).isEqualTo(1)

      inputFlow.value = 2
      assertThat(awaitItem()).isEqualTo(Model(input = 2, presentCalls = 2))
      assertThat(childPresentCalls).isEqualTo(2)
    }
  }

  @Test
  fun `presentDetached emits the current parent model before detached input catches up`() =
    runTest {
      data class ChildModel(val input: Int) : BaseModel
      data class ParentModel(val input: Int, val childInput: Int) : BaseModel

      val childPresenter =
        object : MoleculePresenter<Int, ChildModel> {
          @Composable
          override fun present(input: Int): ChildModel {
            return ChildModel(input)
          }
        }

      val parentPresenter =
        object : MoleculePresenter<Int, ParentModel> {
          @Composable
          override fun present(input: Int): ParentModel {
            val childModel = childPresenter.presentDetached(input)

            return ParentModel(input = input, childInput = childModel.input)
          }
        }

      val inputFlow = MutableStateFlow(1)

      parentPresenter.test(this, inputFlow) {
        assertThat(awaitItem()).isEqualTo(ParentModel(input = 1, childInput = 1))

        inputFlow.value = 2

        assertThat(awaitItem()).isEqualTo(ParentModel(input = 2, childInput = 1))
        assertThat(awaitItem()).isEqualTo(ParentModel(input = 2, childInput = 2))
      }
    }

  @Test
  fun `launching a presenter on a canceled scope throws an error`() = runTest {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    coroutineScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()

    val moleculeScope = MoleculeScope(coroutineScope, RecompositionMode.Immediate)

    data class Model(val value: String) : BaseModel

    val presenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          return Model(input.toString())
        }
      }

    assertFailsWith<IllegalStateException> { moleculeScope.launchMoleculePresenter(presenter, 1) }
  }

  private class ThreadPresenter(private val markedDispatcher: MarkedDispatcher) :
    MoleculePresenter<StateFlow<String>, ThreadPresenter.Model> {

    @Composable
    override fun present(input: StateFlow<String>): Model {
      val value by input.collectAsState()

      return Model(
        value = value,
        ranOnMarkedDispatcher = markedDispatcher.isCurrentThreadMarked(),
      )
    }

    data class Model(val value: String, val ranOnMarkedDispatcher: Boolean) : BaseModel
  }
}
