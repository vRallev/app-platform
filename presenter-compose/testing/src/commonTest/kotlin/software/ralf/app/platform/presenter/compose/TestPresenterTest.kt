package software.ralf.app.platform.presenter.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.internal.IgnoreWasm
import software.ralf.app.platform.presenter.BaseModel

class TestPresenterTest {

  @Test
  fun `values are properly emitted`() = runTest {
    data class Model(val value: String) : BaseModel

    class TestPresenter : ComposePresenter<StateFlow<String>, Model> {
      @Composable
      override fun present(input: StateFlow<String>): Model {
        return Model(input.collectAsState().value)
      }
    }

    val input = MutableStateFlow("a")
    TestPresenter().test(this, input) {
      assertThat(awaitItem().value).isEqualTo("a")

      input.value = "b"
      assertThat(awaitItem().value).isEqualTo("b")
    }
  }

  @Test
  fun `the coroutine context can be changed`() = runTest {
    data class Model(val name: CoroutineName?) : BaseModel

    class TestPresenter : ComposePresenter<Unit, Model> {
      @Composable
      override fun present(input: Unit): Model {
        @OptIn(InternalComposeApi::class)
        return Model(currentComposer.applyCoroutineContext[CoroutineName])
      }
    }

    TestPresenter().test(this, CoroutineName("def")) {
      assertThat(awaitItem().name?.name).isEqualTo("def")
    }
  }

  @Test
  @IgnoreWasm
  fun `failures in a presenter are reported in the first emission`() {
    class Model : BaseModel

    class TestPresenter : ComposePresenter<Unit, Model> {
      @Composable
      override fun present(input: Unit): Model {
        fail("test failure")
      }
    }

    val throwable =
      assertFailsWith<Throwable> {
        runTest {
          TestPresenter().test(this) {
            // This call will not succeed and timeout. Presenters are turned into a
            // StateFlow and StateFlows never complete. Instead, the error is reported
            // in the CoroutineScope running Molecule. That's why it's tearing down the
            // entire test and this is good. A presenter should never throw an exception.
            //
            // This behavior also follows what would happen in production.
            awaitError()
          }
        }
      }
    assertThat(throwable).messageContains("test failure")
  }

  @Test
  @IgnoreWasm
  fun `failures in a presenter are reported in later emissions`() {
    class Model : BaseModel

    val trigger = MutableStateFlow(0)

    class TestPresenter : ComposePresenter<Unit, Model> {
      @Composable
      override fun present(input: Unit): Model {
        val triggerValue by trigger.collectAsState()
        if (triggerValue > 0) {
          fail("test failure")
        }
        return Model()
      }
    }

    var itemReceived = false
    val throwable =
      assertFailsWith<Throwable> {
        runTest {
          TestPresenter().test(this, timeout = 100.milliseconds) {
            assertThat(awaitItem()).isNotNull()
            itemReceived = true

            trigger.value = 1

            // This call will not succeed and timeout. Presenters are turned into a
            // StateFlow and StateFlows never complete. Instead, the error is reported
            // in the CoroutineScope running Molecule. That's why it's tearing down the
            // entire test and this is good. A presenter should never throw an exception.
            //
            // This behavior also follows what would happen in production.
            awaitError()
          }
        }
      }
    assertThat(throwable).messageContains("No value produced in 100ms")
    assertThat(throwable.suppressedExceptions.single()).messageContains("test failure")

    assertThat(itemReceived).isTrue()
  }
}
