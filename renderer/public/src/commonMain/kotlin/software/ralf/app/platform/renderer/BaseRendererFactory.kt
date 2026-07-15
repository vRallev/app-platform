package software.ralf.app.platform.renderer

import kotlin.reflect.KClass
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.kotlinInjectComponent
import software.ralf.app.platform.scope.di.metro.metroDependencyGraph

/**
 * Default implementation for [RendererFactory]. Implementations usually override [createRenderer]
 * and [getRenderer] to configure the [Renderer]s provided by the dependency graph further.
 */
public open class BaseRendererFactory(rootScopeProvider: RootScopeProvider) : RendererFactory {

  private val rendererComponent =
    rootScopeProvider.rootScope
      .kotlinInjectComponentOrNull<RendererComponent.Parent>()
      ?.rendererComponent(this)

  private val rendererGraph =
    rootScopeProvider.rootScope
      .metroDependencyGraphOrNull<RendererGraph.Factory>()
      ?.createRendererGraph(this)

  private val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
    rendererComponent?.renderers.orEmpty() +
      rendererGraph?.renderers.orEmpty().mapValues { { it.value() } }

  private val cacheKeys: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
    rendererComponent?.modelToRendererMapping.orEmpty() +
      rendererGraph?.modelToRendererMapping.orEmpty()

  private val rendererCache = mutableMapOf<CacheKey, Renderer<*>>()
  private val lock = SynchronizedObject()

  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
    @Suppress("UNCHECKED_CAST")
    return checkNotNull(renderers[modelType]?.invoke() as? Renderer<T>) {
      errorMessageForMissingRenderer(modelType)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : BaseModel> getRenderer(modelType: KClass<out T>, rendererId: Int): Renderer<T> {
    val rendererClass =
      checkNotNull(cacheKeys[modelType]) { errorMessageForMissingRenderer(modelType) }

    return synchronized(lock) {
      rendererCache.getOrPut(CacheKey(rendererClass, rendererId)) { createRenderer(modelType) }
    }
      as Renderer<T>
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun <T : BaseModel> errorMessageForMissingRenderer(
    modelType: KClass<out T>
  ): String {
    return "No renderer was provided for $modelType. Did you add @ContributesRenderer?"
  }

  private data class CacheKey(val clazz: KClass<out Renderer<*>>, val rendererId: Int)

  private companion object {
    inline fun <reified T : Any> Scope.metroDependencyGraphOrNull(): T? {
      return try {
        metroDependencyGraph<T>()
      } catch (_: NoSuchElementException) {
        null
      }
    }

    inline fun <reified T : Any> Scope.kotlinInjectComponentOrNull(): T? {
      return try {
        kotlinInjectComponent<T>()
      } catch (_: NoSuchElementException) {
        null
      }
    }
  }
}
