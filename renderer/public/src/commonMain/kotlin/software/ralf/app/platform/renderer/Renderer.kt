package software.ralf.app.platform.renderer

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.Presenter

/**
 * A renderer handles the conversion of a given [BaseModel] into UI. Models are created and updated
 * in the [Presenter] layer.
 *
 * This interface will rarely be used directly and a more specific renderer implementation for a
 * concrete platform should be favored unless there is a specific need.
 *
 * [Renderer]s are composable and can be injected into other renderers to create more complex UI
 * hierarchies. A parent model should contain sub models that map to the required type of the child
 * renderer. In this way Presenter compositions and Renderer compositions often mirror each other.
 *
 * ```
 * data class ParentModel(
 *      val childModel: ChildModel
 * ): BaseModel
 * ```
 *
 * A parent renderer is responsible for initializing and calling [render] on their child renderers.
 *
 * ```
 * @Inject
 * class ParentRenderer(
 *      private val childRenderer: ChildRenderer
 * ): Renderer<MyModel> {
 *
 *      override fun render(model: ParentModel) {
 *          childRenderer.render(model.childModel)
 *      }
 * }
 * ```
 */
public interface Renderer<in ModelT : BaseModel> {
  /** Ask the [Renderer] to render the given model. */
  public fun render(model: ModelT)
}
