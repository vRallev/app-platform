package software.ralf.app.platform.recipes

import android.app.Application
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * The final Android app component. Note that [application] is an Android specific type and classes
 * living in the Android source folder can therefore inject [Application].
 *
 * [rootScopeProvider] is provided in the [AndroidAppComponent] and can be injected.
 */
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AndroidAppComponent(
  @get:Provides val application: Application,
  @get:Provides val rootScopeProvider: RootScopeProvider,
) : AndroidAppComponentMerged
