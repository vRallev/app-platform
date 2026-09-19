package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

class ComposePresenterScopeTest {

  @Test
  fun `canceling a ComposePresenterScope cancels the CoroutineScope`() {
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    val composePresenterScope = ComposePresenterScope(coroutineScope, RecompositionMode.Immediate)

    assertThat(coroutineScope.isActive).isTrue()

    composePresenterScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()
  }
}
