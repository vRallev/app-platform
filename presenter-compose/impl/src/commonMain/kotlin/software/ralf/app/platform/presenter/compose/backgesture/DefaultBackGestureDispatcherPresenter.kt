package software.ralf.app.platform.presenter.compose.backgesture

import dev.zacsweers.metro.AppScope as MetroAppScope
import dev.zacsweers.metro.ContributesTo as MetroContributesTo
import dev.zacsweers.metro.Provides as MetroProvides
import dev.zacsweers.metro.SingleIn as MetroSingleIn
import me.tatarka.inject.annotations.Provides as KiProvides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope as KiAppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo as KiContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn as KiSingleIn

/**
 * Provides a [BackGestureDispatcherPresenter] that maintains a list of registered back gesture
 * listeners and forwards events to the last registered callback. See
 * [BackGestureDispatcherPresenter] for more details.
 */
@KiContributesTo(KiAppScope::class)
public interface DefaultBackGestureDispatcherPresenterComponent {
  /** Provides a [BackGestureDispatcherPresenter] as singleton in the kotlin-inject graph. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideDefaultBackGestureDispatcherPresenter(): BackGestureDispatcherPresenter =
    BackGestureDispatcherPresenter.createNewInstance()
}

/**
 * Provides a [BackGestureDispatcherPresenter] that maintains a list of registered back gesture
 * listeners and forwards events to the last registered callback. See
 * [BackGestureDispatcherPresenter] for more details.
 */
@MetroContributesTo(MetroAppScope::class)
public interface DefaultBackGestureDispatcherPresenterGraph {
  /** Provides a [BackGestureDispatcherPresenter] as singleton in the Metro graph. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideDefaultBackGestureDispatcherPresenter(): BackGestureDispatcherPresenter =
    BackGestureDispatcherPresenter.createNewInstance()
}
