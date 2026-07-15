package software.ralf.app.platform.presenter.molecule

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
 * as fast as possible.
 */
public class WasmJsMoleculeScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    recompositionMode = RecompositionMode.Immediate,
  )

/** Provides the [WasmJsMoleculeScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface WasmJsMoleculeScopeFactoryComponent {
  /** Provides the [WasmJsMoleculeScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideWasmJsMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = WasmJsMoleculeScopeFactory(coroutineScopeFactory)
}

/** Provides the [WasmJsMoleculeScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface WasmJsMoleculeScopeFactoryGraph {
  /** Provides the [WasmJsMoleculeScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideWasmJsMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = WasmJsMoleculeScopeFactory { coroutineScopeFactory() }
}
