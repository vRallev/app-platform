package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope

/**
 * Uses the given [coroutineScope] to create new [ComposePresenterScope] instances. In testing
 * environments often [TestScope] is used as argument.
 */
public class FakeComposePresenterScopeFactory(private val coroutineScope: CoroutineScope) :
  ComposePresenterScopeFactory {
  override fun createComposePresenterScope(): ComposePresenterScope =
    createComposePresenterScopeFromCoroutineScope(coroutineScope)

  override fun createComposePresenterScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
  ): ComposePresenterScope {
    return if (coroutineScope is TestScope) {
      coroutineScope.composePresenterScope(coroutineContext)
    } else {
      ComposePresenterScope(
        coroutineScope = coroutineScope + coroutineContext,
        recompositionMode = RecompositionMode.Immediate,
      )
    }
  }
}
