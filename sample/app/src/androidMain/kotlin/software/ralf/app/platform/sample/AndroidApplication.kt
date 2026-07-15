package software.ralf.app.platform.sample

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope

/**
 * The [Application] class of our sample app. Note that this class implements [RootScopeProvider].
 * This is helpful to get access to the root scope from Android components such as activities.
 */
open class AndroidApplication : Application(), RootScopeProvider {

  private val demoApplication = DemoApplication()

  override val rootScope: Scope
    get() = demoApplication.rootScope

  override fun onCreate() {
    demoApplication.create(metroGraph(demoApplication))
    super.onCreate()
  }

  /** Create the [AppGraph]. In UI tests we use a different instance. */
  protected open fun metroGraph(demoApplication: DemoApplication): AppGraph {
    return createGraphFactory<AndroidAppGraph.Factory>().create(this, demoApplication)
  }
}
