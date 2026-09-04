package software.ralf.app.platform.presenter.compose.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.test

class CommonBackGestureDispatcherPresenterTest {
  @Test
  fun `the back handler is invoked`() = runTest {
    val dispatcher = CommonBackGestureDispatcherPresenter()

    data class Model(val backPressCount: Int) : BaseModel

    val presenter =
      object : ComposePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          var backPressCount by remember { mutableIntStateOf(0) }
          BackHandlerPresenter { backPressCount++ }

          return Model(backPressCount = backPressCount)
        }
      }

    RootPresenter(dispatcher, presenter).test(this) {
      assertThat(awaitItem().backPressCount).isEqualTo(0)

      dispatcher.onPredictiveBack(emptyFlow())
      assertThat(awaitItem().backPressCount).isEqualTo(1)

      dispatcher.onPredictiveBack(emptyFlow())
      assertThat(awaitItem().backPressCount).isEqualTo(2)
    }
  }

  @Test
  fun `the predictive back handler is invoked`() = runTest {
    val dispatcher = CommonBackGestureDispatcherPresenter()

    data class Model(val lastEvent: BackEventPresenter?) : BaseModel

    val presenter =
      object : ComposePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          var lastEvent by remember { mutableStateOf<BackEventPresenter?>(null) }

          PredictiveBackHandlerPresenter { progress -> progress.collect { lastEvent = it } }

          return Model(lastEvent = lastEvent)
        }
      }

    RootPresenter(dispatcher, presenter).test(this) {
      assertThat(awaitItem().lastEvent).isNull()

      dispatcher.onPredictiveBack(
        flowOf(BackEventPresenter(1f, 1f, 1f, BackEventPresenter.EDGE_RIGHT))
      )
      assertThat(awaitItem().lastEvent?.touchX).isEqualTo(1f)
    }
  }

  @Test
  fun `the last enabled handler wins`() = runTest {
    val dispatcher = CommonBackGestureDispatcherPresenter()

    data class Model(val backPressCount1: Int, val backPressCount2: Int) : BaseModel

    val presenter =
      object : ComposePresenter<Unit, Model> {
        @Composable
        override fun present(input: Unit): Model {
          var backPressCount1 by remember { mutableIntStateOf(0) }
          BackHandlerPresenter { backPressCount1++ }

          var backPressCount2 by remember { mutableIntStateOf(0) }
          BackHandlerPresenter(enabled = backPressCount2 == 0) { backPressCount2++ }

          return Model(backPressCount1 = backPressCount1, backPressCount2 = backPressCount2)
        }
      }

    RootPresenter(dispatcher, presenter).test(this) {
      with(awaitItem()) {
        assertThat(backPressCount1).isEqualTo(0)
        assertThat(backPressCount2).isEqualTo(0)
      }

      dispatcher.onPredictiveBack(emptyFlow())
      with(awaitItem()) {
        assertThat(backPressCount1).isEqualTo(0)
        assertThat(backPressCount2).isEqualTo(1)
      }

      dispatcher.onPredictiveBack(emptyFlow())
      with(awaitItem()) {
        assertThat(backPressCount1).isEqualTo(1)
        assertThat(backPressCount2).isEqualTo(1)
      }
    }
  }

  @Test
  fun `not registering BackGestureDispatcherPresenter as composition local throws an error`() =
    runTest {
      data class Model(val backPressCount: Int) : BaseModel

      val presenter =
        object : ComposePresenter<Unit, Model> {
          @Composable
          override fun present(input: Unit): Model {
            var backPressCount by remember { mutableIntStateOf(0) }
            BackHandlerPresenter { backPressCount++ }

            return Model(backPressCount = backPressCount)
          }
        }

      assertFailure { presenter.test(this) {} }
        .messageContains(
          "Couldn't find the BackGestureDispatcherPresenter in the presenter hierarchy. " +
            "Did you register the BackGestureDispatcherPresenter instance as composition local? " +
            "See LocalBackGestureDispatcherPresenter for more details."
        )
    }

  @Test
  fun `the listener count increases with the number of enabled handlers`() =
    runTest(UnconfinedTestDispatcher()) {
      val dispatcher = CommonBackGestureDispatcherPresenter()

      var handlers by mutableIntStateOf(0)
      var disabledHandlers by mutableIntStateOf(0)

      class Model : BaseModel
      val model = Model()

      val presenter =
        object : ComposePresenter<Unit, Model> {
          @Composable
          override fun present(input: Unit): Model {
            repeat(handlers) { index ->
              BackHandlerPresenter(enabled = index >= disabledHandlers) {}
            }

            return model
          }
        }

      RootPresenter(dispatcher, presenter).test(this) {
        skipItems(1)

        assertThat(dispatcher.listenersCount.value).isEqualTo(0)

        handlers = 2
        assertThat(dispatcher.listenersCount.value).isEqualTo(2)

        disabledHandlers = 1
        assertThat(dispatcher.listenersCount.value).isEqualTo(1)
      }
    }

  @Test
  fun `calling onPredictiveBack without a registered handler is an error`() =
    runTest(UnconfinedTestDispatcher()) {
      val dispatcher = CommonBackGestureDispatcherPresenter()

      var handlerEnabled by mutableStateOf(true)

      data class Model(val count: Int) : BaseModel

      val presenter =
        object : ComposePresenter<Unit, Model> {
          @Composable
          override fun present(input: Unit): Model {
            var count by remember { mutableIntStateOf(0) }
            BackHandlerPresenter(enabled = handlerEnabled) { count++ }

            return Model(count)
          }
        }

      RootPresenter(dispatcher, presenter).test(this) {
        assertThat(awaitItem().count).isEqualTo(0)
        assertThat(dispatcher.listenersCount.value).isEqualTo(1)

        dispatcher.onPredictiveBack(emptyFlow())
        assertThat(awaitItem().count).isEqualTo(1)

        handlerEnabled = false
        assertThat(dispatcher.listenersCount.value).isEqualTo(0)

        assertFailure { dispatcher.onPredictiveBack(emptyFlow()) }
          .messageContains(
            "No back gesture listener was registered or they were all disabled. Check " +
              "`listenerCount` before invoking this function."
          )
      }
    }

  private class RootPresenter<ModelT : BaseModel>(
    private val dispatcher: BackGestureDispatcherPresenter,
    private val presenter: ComposePresenter<Unit, ModelT>,
  ) : ComposePresenter<Unit, ModelT> {
    @Composable
    override fun present(input: Unit): ModelT {
      return withCompositionLocal(LocalBackGestureDispatcherPresenter provides dispatcher) {
        presenter.present(Unit)
      }
    }
  }
}
