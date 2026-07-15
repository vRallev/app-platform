package software.ralf.app.platform.presenter

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.ralf.app.platform.scope.coroutine.MainCoroutineDispatcher

/** Provides the coroutine scope to run presenters. */
@ContributesTo(AppScope::class)
public interface PresenterCoroutineScopeComponent {
  /**
   * Bind the app coroutine scope as default scope for presenters to allow them to run as long as
   * the app is alive. The coroutine scope will use the main dispatcher by default, because
   * presenters produce state for the UI and computing their models should have the highest
   * priority.
   */
  @Provides
  @PresenterCoroutineScope
  public fun providePresenterCoroutineScope(
    @ForScope(AppScope::class) scope: CoroutineScope,
    @MainCoroutineDispatcher mainDispatcher: CoroutineDispatcher,
  ): CoroutineScope = scope + mainDispatcher
}
