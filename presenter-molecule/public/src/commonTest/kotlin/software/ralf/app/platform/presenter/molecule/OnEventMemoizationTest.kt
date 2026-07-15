package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DontMemoize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel

class OnEventMemoizationTest {

  @Test
  fun `onEvent lambda is memoized for different inputs`() = runTest {
    var presentCalls = 0

    val presenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          presentCalls++

          return Model(string = if (input == 0) "A" else "B") {}
        }
      }

    val inputs = MutableStateFlow(0)

    presenter.test(this, inputs) {
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(1)

      inputs.value = 1
      assertThat(awaitItem().string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(2)

      inputs.value = 2
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(3)

      inputs.value = 3
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(4)

      // This verifies that the presenter recomposes and a new Model is returned. But due to the
      // strong skipping mode and lambda memoization the new model is equal and no update emitted.
      inputs.value = 0
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(5)
    }
  }

  @Test
  fun `onEvent lambda is memoized for state changes`() = runTest {
    var presentCalls = 0

    var state by mutableIntStateOf(0)

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          presentCalls++

          return Model(string = if (state == 0) "A" else "B") {}
        }
      }

    presenter.test(this) {
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(1)

      state = 1
      assertThat(awaitItem().string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(2)

      state = 2
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(3)

      state = 3
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(4)

      // This verifies that the presenter recomposes and a new Model is returned. But due to the
      // strong skipping mode and lambda memoization the new model is equal and no update emitted.
      state = 0
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(5)
    }
  }

  @Test
  fun `onEvent lambda is memoized for onEvent callbacks`() = runTest {
    var presentCalls = 0

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          presentCalls++

          var state by remember { mutableIntStateOf(0) }

          return Model(string = if (state == 0) "A" else "B") { state = it.value }
        }
      }

    presenter.test(this) {
      var model = awaitItem()
      assertThat(model.string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(1)

      model.onEvent(Event(1))

      model = awaitItem()
      assertThat(model.string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(2)

      model.onEvent(Event(2))
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(3)

      model.onEvent(Event(3))
      runCurrent()
      expectNoEvents()
      assertThat(presentCalls).isEqualTo(4)

      // This verifies that the presenter recomposes and a new Model is returned. But due to the
      // strong skipping mode and lambda memoization the new model is equal and no update emitted.
      model.onEvent(Event(0))
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(5)
    }
  }

  @Test
  fun `a new model is produced when memoization is disabled`() = runTest {
    var presentCalls = 0

    val presenter =
      object : MoleculePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          presentCalls++

          var state by remember { mutableIntStateOf(0) }

          return Model(
            string = if (state == 0) "A" else "B",
            onEvent = @DontMemoize { state = it.value },
          )
        }
      }

    presenter.test(this) {
      var model = awaitItem()
      assertThat(model.string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(1)

      model.onEvent(Event(1))

      model = awaitItem()
      assertThat(model.string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(2)

      model.onEvent(Event(2))

      runCurrent()
      model = awaitItem()
      assertThat(model.string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(3)

      model.onEvent(Event(3))

      runCurrent()
      model = awaitItem()
      assertThat(model.string).isEqualTo("B")
      assertThat(presentCalls).isEqualTo(4)

      // This verifies that the presenter recomposes and a new Model is returned. But due to the
      // strong skipping mode and lambda memoization the new model is equal and no update emitted.
      model.onEvent(Event(0))
      assertThat(awaitItem().string).isEqualTo("A")
      assertThat(presentCalls).isEqualTo(5)
    }
  }

  private data class Event(val value: Int)

  private data class Model(val string: String, val onEvent: (Event) -> Unit) : BaseModel
}
