package software.ralf.app.platform.renderer.metro

import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel

/**
 * DO NOT USE DIRECTLY.
 *
 * This is a multibindings key used in Metro for identifying renderers by their model type. This key
 * is used by our custom code generator for `@ContributesRenderer`. [value] refers to the concrete
 * [BaseModel] handled by the renderer.
 */
@MapKey public annotation class RendererKey(val value: KClass<out BaseModel>)
