package software.ralf.app.platform.presenter.molecule

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
 * Runs `MoleculePresenters` on the main thread provided by [PresenterCoroutineScope] and recomposes
 * only once per screen refresh when needed.
 */
public class IosMoleculeScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = DisplayLinkClock,
    recompositionMode = RecompositionMode.ContextClock,
  )

/** Provides the [IosMoleculeScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface IosMoleculeScopeFactoryComponent {
  /** Provides the [IosMoleculeScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideIosMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = IosMoleculeScopeFactory(coroutineScopeFactory)
}

/** Provides the [IosMoleculeScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface IosMoleculeScopeFactoryGraph {
  /** Provides the [IosMoleculeScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideIosMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = IosMoleculeScopeFactory { coroutineScopeFactory() }
}
