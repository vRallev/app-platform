package software.ralf.app.platform.presenter.compose

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.internal.IgnoreWasm

class FakeComposePresenterScopeFactoryTest {

  @Test
  fun `a created ComposePresenterScope can be canceled`() = runTest {
    val scope = FakeComposePresenterScopeFactory(this).createComposePresenterScope()
    scope.cancel()

    var didRun = false
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { didRun = true }

    assertThat(didRun).isTrue()
    assertThat(scope.coroutineScope.isActive).isFalse()
  }

  @Test
  @IgnoreWasm
  fun `a created ComposePresenterScope does not need to be canceled for the test to complete`() {
    // Basically this test should not hang.
    var presenterJobStarted = false
    runTest {
      val scope = FakeComposePresenterScopeFactory(this).createComposePresenterScope()
      scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
        presenterJobStarted = true
        awaitCancellation()
      }
    }

    assertThat(presenterJobStarted).isTrue()
  }

  @Test
  fun `the coroutine context is added to the scope`() = runTest {
    val scope =
      FakeComposePresenterScopeFactory(this)
        .createComposePresenterScopeFromCoroutineScope(this, CoroutineName("test"))

    val name = scope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")
  }

  @Test
  fun `a decorated TestScope does not create an unmanaged child`() = runTest {
    val testJob = coroutineContext.job
    val originalChildren = testJob.children.toList()

    FakeComposePresenterScopeFactory(this)
      .createComposePresenterScopeFromCoroutineScope(this + CoroutineName("decorated"))

    assertThat(testJob.children.toList()).isEqualTo(originalChildren)
  }

  @Test
  fun `a regular CoroutineScope can be used to create a ComposePresenterScope`() = runTest {
    val coroutineScope = CoroutineScope(CoroutineName("test"))
    val composePresenterScope =
      FakeComposePresenterScopeFactory(this)
        .createComposePresenterScopeFromCoroutineScope(coroutineScope)

    val name = composePresenterScope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")

    composePresenterScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }

  @Test
  fun `a regular CoroutineScope creates a ComposePresenterScope without a TestScope`() {
    val coroutineScope = CoroutineScope(CoroutineName("test"))
    val composePresenterScope =
      FakeComposePresenterScopeFactory(coroutineScope).createComposePresenterScope()

    val name = composePresenterScope.coroutineScope.coroutineContext[CoroutineName.Key]
    assertThat(name?.name).isEqualTo("test")

    composePresenterScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }
}
