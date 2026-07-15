package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.ViewGroup
import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider

/** [RendererFactory] that is able to create [ViewRenderer] instances for Android. */
public open class AndroidRendererFactory(
  rootScopeProvider: RootScopeProvider,
  private val activity: Activity,
  private val parent: ViewGroup,
) : BaseRendererFactory(rootScopeProvider) {

  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
    return createRenderer(modelType, parent)
  }

  /**
   * Allows you to override the parent ViewGroup passed in as constructor argument. This is helpful
   * for renderers that embed other renderers and want to change the parent ViewGroup for them.
   *
   * @see [createRenderer] for more details.
   */
  public fun <T : BaseModel> createRenderer(
    modelType: KClass<out T>,
    parent: ViewGroup,
  ): Renderer<T> {
    val renderer = super.createRenderer(modelType)

    if (renderer is BaseAndroidViewRenderer<*>) {
      renderer.init(activity, parent)
    }

    return renderer
  }

  override fun <T : BaseModel> getRenderer(modelType: KClass<out T>, rendererId: Int): Renderer<T> {
    return getRenderer(modelType, parent, rendererId)
  }

  /**
   * Allows you to override the parent ViewGroup passed in as constructor argument. This is helpful
   * for renderers that embed other renderers and want to change the parent ViewGroup for them.
   *
   * @see [getRenderer] for more details.
   */
  public fun <T : BaseModel> getRenderer(
    modelType: KClass<out T>,
    parent: ViewGroup,
    rendererId: Int = 0,
  ): Renderer<T> {
    // For different parents we must return unique renderers. Therefore, include the parent in the
    // rendererId.
    val finalRendererId = parent.hashCode() + rendererId

    val renderer = super.getRenderer(modelType, finalRendererId)

    if (renderer is BaseAndroidViewRenderer<*>) {
      renderer.init(activity, parent)
    }

    return renderer
  }
}

/**
 * Convenience function to pass in [parent] to [RendererFactory.createRenderer], if it's an
 * [AndroidRendererFactory]. Usually, the parent needs to be changed when a [Renderer] embeds
 * another [Renderer].
 */
public fun <T : BaseModel> RendererFactory.createRenderer(
  modelType: KClass<out T>,
  parent: ViewGroup,
): Renderer<T> {
  return if (this is AndroidRendererFactory) {
    createRenderer(modelType, parent)
  } else {
    createRenderer(modelType)
  }
}

/**
 * Convenience function to pass in [parent] to [RendererFactory.getRenderer], if it's an
 * [AndroidRendererFactory]. Usually, the parent needs to be changed when a [Renderer] embeds
 * another [Renderer].
 */
public fun <T : BaseModel> RendererFactory.getRenderer(
  modelType: KClass<out T>,
  parent: ViewGroup,
  rendererId: Int = 0,
): Renderer<T> {
  return if (this is AndroidRendererFactory) {
    getRenderer(modelType, parent, rendererId)
  } else {
    getRenderer(modelType, rendererId)
  }
}

/**
 * Creates and caches the returned [Renderer] with the given [parent] as renderer ID. This implies a
 * one-to-one relationship between the returned [Renderer] and the [parent] view.
 *
 * This function should generally only be used, when the returned [Renderer] is part of a
 * `RecyclerView`. All child views of the recycled views should be cached for efficiency. This is
 * what this function achieves by having the one-to-one relationship.
 *
 * Another use case is showing multiple renderers of the same type on the same screen simultaneously
 * and their identity is determined by the position in the layout or on other words by the [parent]
 * view.
 */
@Deprecated(
  message = "getRenderer() takes the parent into account for caching.",
  replaceWith = ReplaceWith("getRenderer(modelType, parent)"),
  level = DeprecationLevel.WARNING,
)
public fun <T : BaseModel> RendererFactory.getChildRendererForParent(
  modelType: KClass<out T>,
  parent: ViewGroup,
): Renderer<T> {
  val rendererId = parent.hashCode()

  return if (this is AndroidRendererFactory) {
    getRenderer(modelType, parent, rendererId)
  } else {
    getRenderer(modelType, rendererId)
  }
}

/**
 * Creates and caches the returned [Renderer] with the given [parent] as renderer ID. This implies a
 * one-to-one relationship between the returned [Renderer] and the [parent] view.
 *
 * This function should generally only be used, when the returned [Renderer] is part of a
 * `RecyclerView`. All child views of the recycled views should be cached for efficiency. This is
 * what this function achieves by having the one-to-one relationship.
 *
 * Another use case is showing multiple renderers of the same type on the same screen simultaneously
 * and their identity is determined by the position in the layout or on other words by the [parent]
 * view.
 */
@Deprecated(
  message = "getRenderer() takes the parent into account for caching.",
  replaceWith = ReplaceWith("getRenderer(modelType, parent)"),
  level = DeprecationLevel.WARNING,
)
public fun <T : BaseModel> RendererFactory.getChildRendererForParent(
  model: T,
  parent: ViewGroup,
): Renderer<T> = getRenderer(model::class, parent)
