package software.ralf.app.platform.listdetail

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope

/** Android application that creates the shared root scope. */
open class AndroidApplication : Application(), RootScopeProvider {
  private val listDetailApplication = software.ralf.app.platform.listdetail.Application()

  override val rootScope: Scope
    get() = listDetailApplication.rootScope

  override fun onCreate() {
    listDetailApplication.create(metroGraph(listDetailApplication))
    super.onCreate()
  }

  /** Creates the production graph. Tests can override this to install a test graph. */
  protected open fun metroGraph(
    listDetailApplication: software.ralf.app.platform.listdetail.Application
  ): AppGraph {
    return createGraphFactory<AndroidAppGraph.Factory>().create(this, listDetailApplication)
  }
}
