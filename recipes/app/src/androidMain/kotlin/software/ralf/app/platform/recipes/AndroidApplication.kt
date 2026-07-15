package software.ralf.app.platform.recipes

import android.app.Application
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope

/**
 * The [Application] class of our recipes app. Note that this class implements [RootScopeProvider].
 * This is helpful to get access to the root scope from Android components such as activities.
 */
open class AndroidApplication : Application(), RootScopeProvider {

  private val demoApplication = DemoApplication()

  override val rootScope: Scope
    get() = demoApplication.rootScope

  override fun onCreate() {
    demoApplication.create(component(demoApplication))
    super.onCreate()
  }

  /** Create the [AppComponent]. In UI tests we use a different instance. */
  protected open fun component(demoApplication: DemoApplication): AppComponent {
    return AndroidAppComponent::class.create(this, demoApplication)
  }
}
