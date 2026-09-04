package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

class DefaultComposePresenterScopeFactoryTest {
  @Test
  fun `the default provided coroutine scope is used when creating a new ComposePresenterScope`() {
    val factory = factory(scope = CoroutineScope(CoroutineName("abc")))
    val composePresenterScope = factory.createComposePresenterScope()

    assertThat(composePresenterScope.name).isEqualTo("abc")
  }

  @Test
  fun `the given coroutine scope is used when creating a new ComposePresenterScope`() {
    val factory = factory(scope = CoroutineScope(CoroutineName("abc")))
    val composePresenterScope =
      factory.createComposePresenterScopeFromCoroutineScope(CoroutineScope(CoroutineName("def")))

    assertThat(composePresenterScope.name).isEqualTo("def")
  }

  @Test
  fun `default coroutine context elements are applied when creating a new ComposePresenterScope`() {
    val factory = factory(coroutineContext = CoroutineName("abc"))
    val composePresenterScope = factory.createComposePresenterScope()

    assertThat(composePresenterScope.name).isEqualTo("abc")
  }

  @Test
  fun `given coroutine context elements override defaults when creating a ComposePresenterScope`() {
    val factory = factory(coroutineContext = CoroutineName("abc"))
    val composePresenterScope =
      factory.createComposePresenterScopeFromCoroutineScope(
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        coroutineContext = CoroutineName("def"),
      )

    assertThat(composePresenterScope.name).isEqualTo("def")
  }

  private fun factory(
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) =
    DefaultComposePresenterScopeFactory(
      coroutineScopeFactory = { scope },
      coroutineContext = coroutineContext,
      recompositionMode = RecompositionMode.Immediate,
    )

  private val ComposePresenterScope.name: String
    get() = requireNotNull(coroutineScope.coroutineContext[CoroutineName.Key]?.name)
}
