package software.ralf.app.platform.renderer

import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * A [RendererFactory] specific to [ComposeRenderer]s. It can only handle [ComposeRenderer]
 * implementations, because they're integrated differently into each runtime.
 */
public class ComposeRendererFactory(rootScopeProvider: RootScopeProvider) :
  BaseRendererFactory(rootScopeProvider) {

  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): ComposeRenderer<T> {
    return super.createRenderer(modelType).asComposeRenderer(modelType)
  }

  override fun <T : BaseModel> getRenderer(
    modelType: KClass<out T>,
    rendererId: Int,
  ): ComposeRenderer<T> {
    return super.getRenderer(modelType, rendererId).asComposeRenderer(modelType)
  }

  private fun <T : BaseModel> Renderer<T>.asComposeRenderer(
    modelType: KClass<out T>
  ): ComposeRenderer<T> {
    check(this is ComposeRenderer<T>) {
      "Expected a ComposeRenderer for model type $modelType, " +
        "but found $this of type ${this::class}."
    }
    return this
  }
}

/**
 * Convenience function to create [BaseComposeRenderer] for the given [modelType]. This is helpful
 * to call from another [BaseComposeRenderer] to embed a child renderer.
 */
public fun <T : BaseModel> RendererFactory.createComposeRenderer(
  modelType: KClass<out T>
): BaseComposeRenderer<T> = createRenderer(modelType).asBaseComposeRenderer()

/**
 * Convenience function to create [BaseComposeRenderer] for the given [model]. This is helpful to
 * call from another [BaseComposeRenderer] to embed a child renderer.
 */
public fun <T : BaseModel> RendererFactory.createComposeRenderer(model: T): BaseComposeRenderer<T> =
  createComposeRenderer(model::class)

/**
 * Convenience function to get [BaseComposeRenderer] for the given [modelType]. This is helpful to
 * call from another [BaseComposeRenderer] to embed a child renderer.
 *
 * [rendererId] allows you to change the cache key and cache multiple renderers for the same
 * [BaseModel] type.
 */
public fun <T : BaseModel> RendererFactory.getComposeRenderer(
  modelType: KClass<out T>,
  rendererId: Int = 0,
): BaseComposeRenderer<T> = getRenderer(modelType, rendererId).asBaseComposeRenderer()

/**
 * Convenience function to get [BaseComposeRenderer] for the given [model]. This is helpful to call
 * from another [BaseComposeRenderer] to embed a child renderer.
 *
 * [rendererId] allows you to change the cache key and cache multiple renderers for the same
 * [BaseModel] type.
 */
public fun <T : BaseModel> RendererFactory.getComposeRenderer(
  model: T,
  rendererId: Int = 0,
): BaseComposeRenderer<T> = getComposeRenderer(model::class, rendererId)

private fun <T : BaseModel> Renderer<T>.asBaseComposeRenderer(): BaseComposeRenderer<T> {
  check(this is BaseComposeRenderer<*>) {
    "The renderer ${this::class} is not an instance of BaseComposeRenderer. " +
      "For Android View and Compose UI interop use ComposeAndroidRendererFactory."
  }

  @Suppress("UNCHECKED_CAST")
  return this as BaseComposeRenderer<T>
}
