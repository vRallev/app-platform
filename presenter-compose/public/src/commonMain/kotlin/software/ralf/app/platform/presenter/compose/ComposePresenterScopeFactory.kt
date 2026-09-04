package software.ralf.app.platform.presenter.compose

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

/** Creates new [ComposePresenterScope] instances. */
public interface ComposePresenterScopeFactory {

  /**
   * Creates a new [ComposePresenterScope]. Once the returned scope is not needed anymore, you must
   * call [ComposePresenterScope.cancel] to avoid memory leaks.
   */
  public fun createComposePresenterScope(): ComposePresenterScope

  /**
   * Wraps the given [coroutineScope] in a [ComposePresenterScope] and applies platform specific
   * defaults in order to run Molecule. [coroutineContext] allows you to add additional elements to
   * the used [CoroutineScope] and override the platform defaults if necessary.
   */
  public fun createComposePresenterScopeFromCoroutineScope(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ): ComposePresenterScope
}
