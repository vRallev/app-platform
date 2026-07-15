package software.ralf.app.platform.presenter.molecule.backgesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.test

class TestBackGestureDispatcherPresenterTest {
  @Test
  fun `a presenter cannot be tested without the dispatcher wrapper`() = runTest {
    val presenter =
      object : MoleculePresenter<Unit, BaseModel> {
        @Composable
        override fun present(input: Unit): BaseModel {
          BackHandlerPresenter {}
          return object : BaseModel {}
        }
      }

    assertFailure { presenter.test(this) {} }
      .messageContains(
        "Couldn't find the BackGestureDispatcherPresenter in the presenter hierarchy."
      )
  }

  @Test
  fun `a presenter can be tested with the dispatcher wrapper`() = runTest {
    val presenter =
      object : MoleculePresenter<Unit, BaseModel> {
        @Composable
        override fun present(input: Unit): BaseModel {
          BackHandlerPresenter {}
          return object : BaseModel {}
        }
      }

    presenter.withBackGestureDispatcher().test(this) { awaitItem() }
  }

  @Test
  fun `back press events can be provided in tests`() =
    runTest(UnconfinedTestDispatcher()) {
      data class Model(val count: Int) : BaseModel

      val presenter =
        object : MoleculePresenter<Unit, Model> {
          @Composable
          override fun present(input: Unit): Model {
            var backPressCount by remember { mutableIntStateOf(0) }
            BackHandlerPresenter { backPressCount++ }
            return Model(backPressCount)
          }
        }

      val backEvents = MutableSharedFlow<Unit>()
      presenter.withBackGestureDispatcher(backEvents).test(this) {
        assertThat(awaitItem().count).isEqualTo(0)

        backEvents.emit(Unit)
        assertThat(awaitItem().count).isEqualTo(1)

        backEvents.emit(Unit)
        assertThat(awaitItem().count).isEqualTo(2)
      }
    }

  @Test
  fun `predictive back press events can be provided in tests`() =
    runTest(UnconfinedTestDispatcher()) {
      data class Model(val eventCount: Int, val doneCount: Int) : BaseModel

      val presenter =
        object : MoleculePresenter<Unit, Model> {
          @Composable
          override fun present(input: Unit): Model {
            var eventCount by remember { mutableIntStateOf(0) }
            var doneCount by remember { mutableIntStateOf(0) }

            PredictiveBackHandlerPresenter { progress ->
              try {
                progress.collect { eventCount++ }
                doneCount++
              } catch (_: CancellationException) {}
            }
            return Model(eventCount = eventCount, doneCount = doneCount)
          }
        }

      val backEvents = MutableSharedFlow<Flow<BackEventPresenter>>()
      presenter.withBackGestureDispatcher(backEvents).test(this) {
        with(awaitItem()) {
          assertThat(eventCount).isEqualTo(0)
          assertThat(doneCount).isEqualTo(0)
        }

        backEvents.emit(
          flow {
            emit(BackEventPresenter(1f, 1f, 1f, BackEventPresenter.EDGE_LEFT))
            delay(1.seconds)
            emit(BackEventPresenter(1f, 1f, 1f, BackEventPresenter.EDGE_LEFT))
          }
        )

        assertThat(awaitItem()).isEqualTo(Model(eventCount = 1, doneCount = 0))

        advanceTimeBy(1.seconds)
        assertThat(awaitItem()).isEqualTo(Model(eventCount = 2, doneCount = 0))
        assertThat(awaitItem()).isEqualTo(Model(eventCount = 2, doneCount = 1))

        backEvents.emit(
          flowOf(
            BackEventPresenter(1f, 1f, 1f, BackEventPresenter.EDGE_LEFT),
            BackEventPresenter(1f, 1f, 1f, BackEventPresenter.EDGE_LEFT),
          )
        )
        assertThat(awaitItem()).isEqualTo(Model(eventCount = 3, doneCount = 1))
        assertThat(awaitItem()).isEqualTo(Model(eventCount = 4, doneCount = 1))
        assertThat(awaitItem()).isEqualTo(Model(eventCount = 4, doneCount = 2))
      }
    }
}
