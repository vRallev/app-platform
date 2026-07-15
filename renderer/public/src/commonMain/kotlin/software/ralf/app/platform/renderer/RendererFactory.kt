package software.ralf.app.platform.renderer

import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel

/** Responsible for initializing a [Renderer] for a given model. */
public interface RendererFactory {
  /**
   * Creates a new [Renderer] for the given [BaseModel] type. This function always creates a new
   * [Renderer] and the result is never cached.
   */
  public fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T>

  /**
   * Obtains the [Renderer] for the given [BaseModel] type. The result is either a new [Renderer] if
   * it wasn't requested before or a cached result from the previous request. Which [Renderer] is
   * returned is based on [modelType]. [rendererId] allows you to change the cache key and cache
   * multiple renderers for the same [BaseModel] type.
   */
  public fun <T : BaseModel> getRenderer(modelType: KClass<out T>, rendererId: Int = 0): Renderer<T>
}

/**
 * Creates a new [Renderer] for the given [BaseModel]. This function always creates a new [Renderer]
 * and the result is never cached.
 */
public fun <T : BaseModel> RendererFactory.createRenderer(model: T): Renderer<T> =
  createRenderer(model::class)

/**
 * Obtains the [Renderer] for the given [BaseModel]. The result is either a new [Renderer] if it
 * wasn't requested before or a cached result from the previous request. Which [Renderer] is
 * returned is based on the type of [model]. [rendererId] allows you to change the cache key and
 * cache multiple renderers for the same [BaseModel] type.
 */
public fun <T : BaseModel> RendererFactory.getRenderer(model: T, rendererId: Int = 0): Renderer<T> =
  getRenderer(model::class, rendererId)
