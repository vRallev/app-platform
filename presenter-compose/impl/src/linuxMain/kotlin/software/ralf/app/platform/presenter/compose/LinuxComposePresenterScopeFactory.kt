package software.ralf.app.platform.presenter.compose

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
 * as fast as possible.
 */
public class LinuxComposePresenterScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  ComposePresenterScopeFactory by DefaultComposePresenterScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    recompositionMode = RecompositionMode.Immediate,
  )

/** Provides the [LinuxComposePresenterScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface LinuxComposePresenterScopeFactoryComponent {
  /** Provides the [LinuxComposePresenterScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideLinuxComposePresenterScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): ComposePresenterScopeFactory = LinuxComposePresenterScopeFactory(coroutineScopeFactory)
}

/** Provides the [LinuxComposePresenterScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface LinuxComposePresenterScopeFactoryGraph {
  /** Provides the [LinuxComposePresenterScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideLinuxComposePresenterScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): ComposePresenterScopeFactory = LinuxComposePresenterScopeFactory { coroutineScopeFactory() }
}
