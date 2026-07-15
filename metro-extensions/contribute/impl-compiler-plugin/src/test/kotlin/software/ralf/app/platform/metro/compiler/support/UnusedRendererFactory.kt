package software.ralf.app.platform.metro.compiler.support

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererFactory

object UnusedRendererFactory : RendererFactory {
  override fun <T : BaseModel> createRenderer(
    modelType: kotlin.reflect.KClass<out T>
  ): Renderer<T> {
    error("unused")
  }

  override fun <T : BaseModel> getRenderer(
    modelType: kotlin.reflect.KClass<out T>,
    rendererId: Int,
  ): Renderer<T> {
    error("unused")
  }
}
