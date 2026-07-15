package software.ralf.app.platform.presenter

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.StateInTest.TestPresenter.Model

@OptIn(ExperimentalCoroutinesApi::class)
class StateInTest {

  @Test
  fun `upstream collections are canceled 'model' is no longer collected`() =
    runTest(UnconfinedTestDispatcher()) {
      val flow = MutableSharedFlow<Int>()
      val presenter = TestPresenter(flow, backgroundScope)

      presenter.model.test {
        assertThat(presenter.startCalled).isEqualTo(1)
        assertThat(presenter.completeCalled).isEqualTo(0)

        assertThat(awaitItem().number).isEqualTo(0)
        flow.emit(1)
        assertThat(awaitItem().number).isEqualTo(1)
        flow.emit(2)
        assertThat(awaitItem().number).isEqualTo(2)

        assertThat(presenter.completeCalled).isEqualTo(0)
      }

      assertThat(presenter.startCalled).isEqualTo(1)
      assertThat(presenter.completeCalled).isEqualTo(1)
    }

  @Test
  fun `collecting multiple times returns the cached value from the StateFlow`() =
    runTest(UnconfinedTestDispatcher()) {
      val flow = MutableSharedFlow<Int>()
      val presenter = TestPresenter(flow, backgroundScope)

      repeat(10) { index ->
        assertThat(presenter.startCalled).isEqualTo(index)
        assertThat(presenter.completeCalled).isEqualTo(index)

        presenter.model.test {
          assertThat(presenter.startCalled).isEqualTo(index + 1)
          assertThat(presenter.completeCalled).isEqualTo(index)

          if (index == 0) {
            // That's the default value.
            assertThat(awaitItem().number).isEqualTo(0)

            flow.emit(1)
          }

          // This value comes from the passed in Flow in the first iteration and
          // then is cached in the StateFlow.
          assertThat(awaitItem().number).isEqualTo(1)
        }

        assertThat(presenter.startCalled).isEqualTo(index + 1)
        assertThat(presenter.completeCalled).isEqualTo(index + 1)
      }
    }

  private class TestPresenter(flow: Flow<Int>, coroutineScope: CoroutineScope) : Presenter<Model> {
    var startCalled = 0
      private set

    var completeCalled = 0
      private set

    override val model: StateFlow<Model> =
      flow
        .map { Model(it) }
        .onStart { startCalled++ }
        .onCompletion { completeCalled++ }
        .stateInPresenter(coroutineScope) { Model(0) }

    data class Model(val number: Int) : BaseModel
  }
}
