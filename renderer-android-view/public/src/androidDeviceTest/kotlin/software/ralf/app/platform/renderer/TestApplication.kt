package software.ralf.app.platform.renderer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.coroutine.CoroutineScopeScoped
import software.ralf.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.ralf.app.platform.scope.di.addKotlinInjectComponent

class TestApplication : Application(), RootScopeProvider {

  var rendererComponent: RendererComponent? = null

  override val rootScope: Scope = Scope.buildRootScope {
    addKotlinInjectComponent(Component())
    addCoroutineScopeScoped(CoroutineScopeScoped(Job() + CoroutineName("test")))
  }

  private inner class Component : RendererComponent.Parent {
    override fun rendererComponent(factory: RendererFactory): RendererComponent =
      requireNotNull(rendererComponent)
  }
}

val testApplication: TestApplication
  get() =
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApplication
