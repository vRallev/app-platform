package software.ralf.app.platform.presenter.compose

import app.cash.molecule.DisplayLinkClock
import app.cash.molecule.RecompositionMode
import dev.zacsweers.metro.AppScope as MetroAppScope
import dev.zacsweers.metro.ContributesTo as MetroContributesTo
import dev.zacsweers.metro.Provides as MetroProvides
import dev.zacsweers.metro.SingleIn as MetroSingleIn
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides as KiProvides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope as KiAppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo as KiContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn as KiSingleIn
import software.ralf.app.platform.presenter.PresenterCoroutineScope

/**
 * Runs `ComposePresenters` on the main thread provided by [PresenterCoroutineScope] and recomposes
 * only once per screen refresh when needed.
 */
public class IosComposePresenterScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  ComposePresenterScopeFactory by DefaultComposePresenterScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = DisplayLinkClock,
    recompositionMode = RecompositionMode.ContextClock,
  )

/** Provides the [IosComposePresenterScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface IosComposePresenterScopeFactoryComponent {
  /** Provides the [IosComposePresenterScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideIosComposePresenterScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): ComposePresenterScopeFactory = IosComposePresenterScopeFactory(coroutineScopeFactory)
}

/** Provides the [IosComposePresenterScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface IosComposePresenterScopeFactoryGraph {
  /** Provides the [IosComposePresenterScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideIosComposePresenterScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): ComposePresenterScopeFactory = IosComposePresenterScopeFactory { coroutineScopeFactory() }
}
