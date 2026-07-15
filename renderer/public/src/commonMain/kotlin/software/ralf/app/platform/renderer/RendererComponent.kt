package software.ralf.app.platform.renderer

import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.ralf.app.platform.presenter.BaseModel

/** Component that provides all [Renderer] instance from the kotlin-inject dependency graph. */
@ContributesSubcomponent(RendererScope::class)
@SingleIn(RendererScope::class)
public interface RendererComponent {
  /** All [Renderer]s provided in the dependency graph. */
  public val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>>

  /**
   * [RendererFactory]s cache renderers based on the model type. This works well, when there's a one
   * to one relationship between a model type and a renderer. However, for sealed hierarchies there
   * are multiple model types pointing to the same renderer.
   *
   * This map, given any model type as key, returns the type that should be used as key for caching.
   * For non-sealed hierarchies (aka a single model type per renderer) there is only single mapping
   * between the model and renderer class. For sealed hierarchies there are multiple entries with
   * all keys pointing to the same renderer class.
   */
  @ForScope(RendererScope::class)
  public val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>>

  /** The parent interface to create a [RendererComponent]. */
  @ContributesSubcomponent.Factory(AppScope::class)
  public interface Parent {
    /** Creates a new [RendererComponent]. */
    public fun rendererComponent(factory: RendererFactory): RendererComponent
  }
}
