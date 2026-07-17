package software.ralf.app.platform.template

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope

/**
 * The [Application] class of our sample app. Note that this class implements [RootScopeProvider].
 * This is helpful to get access to the root scope from Android components such as activities.
 */
open class AndroidApplication : Application(), RootScopeProvider {
  private val templateApplication = software.ralf.app.platform.template.Application()

  override val rootScope: Scope
    get() = templateApplication.rootScope

  override fun onCreate() {
    templateApplication.create(metroGraph(templateApplication))
    super.onCreate()
  }

  /** Create the [AppGraph]. In UI tests we use a different instance. */
  protected open fun metroGraph(
    templateApplication: software.ralf.app.platform.template.Application
  ): AppGraph {
    return createGraphFactory<AndroidAppGraph.Factory>().create(this, templateApplication)
  }
}
