package software.ralf.app.platform.presenter.compose

import app.cash.molecule.RecompositionMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import software.ralf.app.platform.presenter.PresenterCoroutineScope

/**
 * Creates new [ComposePresenterScope]s with the given defaults. When calling
 * [createComposePresenterScope], then [coroutineScopeFactory] is used as default scope.
 * [coroutineContext] allows you to add additional elements to created scopes. [recompositionMode]
 * is used for launching [ComposePresenter]s.
 */
internal class DefaultComposePresenterScopeFactory(
  @PresenterCoroutineScope private val coroutineScopeFactory: () -> CoroutineScope,
  private val coroutineContext: CoroutineContext = EmptyCoroutineContext,
  private val recompositionMode: RecompositionMode,
) : ComposePresenterScopeFactory {

  override fun createComposePresenterScope(): ComposePresenterScope =
    createComposePresenterScopeFromCoroutineScope(coroutineScopeFactory())

  override fun createComposePresenterScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
  ): ComposePresenterScope =
    ComposePresenterScope(
      coroutineScope = coroutineScope + this.coroutineContext + coroutineContext,
      recompositionMode = recompositionMode,
    )
}
