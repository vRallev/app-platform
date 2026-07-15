package software.ralf.app.platform.recipes

import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.ralf.app.platform.presenter.molecule.MoleculeScopeFactory
import software.ralf.app.platform.recipes.swiftui.SwiftUiHomePresenter
import software.ralf.app.platform.scope.Scoped
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped

/**
 * Shared interface for the app component. The final components live in the platform specific source
 * folders in order to have access to platform specific code.
 */
@ContributesTo(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
  /** All [Scoped] instances part of the app scope. */
  @ForScope(AppScope::class) val appScopedInstances: Set<Scoped>

  /** The coroutine scope that runs as long as the app scope is alive. */
  @ForScope(AppScope::class) val appScopeCoroutineScopeScoped: CoroutineScopeScoped

  /**
   * Provide at least one implementation in the scope, otherwise kotlin-inject will complain. The
   * recipes app actually doesn't have a [Scoped] instance in the app scope, that's why this is
   * needed.
   */
  @Provides @IntoSet @ForScope(AppScope::class) fun provideEmptyScoped(): Scoped = Scoped.NO_OP

  /** The root presenter for the SwiftUI recipe. */
  val swiftUiHomePresenter: SwiftUiHomePresenter

  /** Factory needed to launch presenters from native. */
  val moleculeScopeFactory: MoleculeScopeFactory
}
