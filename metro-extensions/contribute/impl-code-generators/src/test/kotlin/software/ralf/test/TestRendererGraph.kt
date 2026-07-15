package software.ralf.test

import dev.zacsweers.metro.ForScope
import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererScope

interface TestRendererGraph {
  val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>>

  @ForScope(RendererScope::class)
  val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>>
}
