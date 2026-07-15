package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.ViewGroup
import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider

/**
 * [RendererFactory] that is able to create [ComposeRenderer] and [ViewRenderer] instances for
 * Android, unlike [ComposeRendererFactory] which only handles [ComposeRenderer] and unlike
 * [AndroidRendererFactory] which only handles [ViewRenderer]. Further, this implementation provides
 * adapters for renderers for a seamless interop between Compose UI and Android Views.
 *
 * Call either [createForComposeUi] or [createForAndroidViews] to create a new instance.
 */
public sealed interface ComposeAndroidRendererFactory : RendererFactory {

  private class ComposeAndroidRendererFactoryComposeUi(rootScopeProvider: RootScopeProvider) :
    BaseRendererFactory(rootScopeProvider), ComposeAndroidRendererFactory {
    override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
      return wrapRenderer(super.createRenderer(modelType), modelType)
    }
  }

  private class ComposeAndroidRendererFactoryAndroidView(
    rootScopeProvider: RootScopeProvider,
    activity: Activity,
    parent: ViewGroup,
  ) : AndroidRendererFactory(rootScopeProvider, activity, parent), ComposeAndroidRendererFactory {
    override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
      return wrapRenderer(super.createRenderer(modelType), modelType)
    }
  }

  public companion object {
    /**
     * Creates a new [RendererFactory] that implements interop for Compose UI and Android Views.
     *
     * Use this function only when you know that `Renderers` returned by this factory are instances
     * of [ComposeRenderer]. This avoids unnecessary wrapping of root `Renderers` and allows you to
     * embed them into a Compose UI hierarchy. Unlike [createForAndroidViews] it's not necessary to
     * provide a parent view.
     *
     * This constraint only applies to root `Renderers`. Child `Renderers` can still be instances of
     * [ViewRenderer] and are wrapped for interop when necessary.
     *
     * A typical pattern looks like this:
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *
     *     val rendererFactory =
     *         ComposeAndroidRendererFactory.createForComposeUi(rootScopeProvider = ...)
     *
     *     setContent {
     *         val model by models.collectAsState()
     *
     *         val renderer = rendererFactory.getComposeRenderer(model)
     *         renderer.renderCompose(model)
     *     }
     * }
     * ```
     */
    public fun createForComposeUi(
      rootScopeProvider: RootScopeProvider
    ): ComposeAndroidRendererFactory = ComposeAndroidRendererFactoryComposeUi(rootScopeProvider)

    /**
     * Creates a new [RendererFactory] that implements interop for Compose UI and Android Views.
     *
     * Use this function when `Renderers` returned by this factory are instances of
     * [ComposeRenderer] or [ViewRenderer]. `Renderers` are wrapped for interop when necessary and
     * embedded as children in [parent].
     *
     * A typical pattern looks like this:
     * ```
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *     setContentView(R.layout.activity_main)
     *
     *     val rendererFactory =
     *         ComposeAndroidRendererFactory(
     *             rootScopeProvider = ...,
     *             activity = this,
     *             parent = findViewById(R.id.main_container),
     *         )
     *
     *     lifecycleScope.launch {
     *         repeatOnLifecycle(Lifecycle.State.STARTED) {
     *             models.collect { model ->
     *                val renderer = rendererFactory.getRenderer(model)
     *                renderer.render(model)
     *             }
     *         }
     *     }
     * }
     * ```
     */
    public fun createForAndroidViews(
      rootScopeProvider: RootScopeProvider,
      activity: Activity,
      parent: ViewGroup,
    ): ComposeAndroidRendererFactory =
      ComposeAndroidRendererFactoryAndroidView(rootScopeProvider, activity, parent)

    private fun <T : BaseModel> wrapRenderer(
      renderer: Renderer<T>,
      modelType: KClass<out T>,
    ): Renderer<T> {
      check(renderer !is ComposeWithinAndroidViewRenderer) {
        "Trying to wrap a render that has been wrapped already for model $modelType."
      }

      check(renderer !is AndroidViewWithinComposeRenderer) {
        "Trying to wrap a render that has been wrapped already for model $modelType."
      }

      return when (renderer) {
        // Wrap a ComposeRenderer to support embedding it in an Android View hierarchy.
        is BaseComposeRenderer<*> ->
          @Suppress("UNCHECKED_CAST")
          ComposeWithinAndroidViewRenderer(renderer as BaseComposeRenderer<T>)

        // Wrap a ViewRenderer to support embedding it in a Compose UI hierarchy.
        is BaseAndroidViewRenderer -> AndroidViewWithinComposeRenderer(renderer)

        // Should not happen, each original Renderer is wrapped.
        else -> error("Unsupported renderer type ${renderer::class} for model $modelType.")
      }
    }
  }
}
