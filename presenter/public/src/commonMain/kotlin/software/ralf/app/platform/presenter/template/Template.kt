package software.ralf.app.platform.presenter.template

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.Presenter

/**
 * Templates connect the presenter with the GUI side of the application and it’s what the UI
 * rendering understands. Practically, a template is one particular type of [BaseModel] that hosts
 * other models (a container of models). However, instead of using a weak type like
 * `List<BaseModel>`, a template carries some semantics around what content should be rendered, how
 * many layers there are and where each individual model should be displayed.
 *
 * The UI rendering understands what templates are, extracts the models, lays them out as needed for
 * a particular screen configuration and renders the content within the respective UI containers.
 *
 * For every single state change in the application a new template containing new, immutable models
 * is emitted. The UI renderer receives the update and renders the new template with the new content
 * on screen.
 *
 * For a [Presenter] to change the used template its model should implement [ModelDelegate] and wrap
 * its model in a [Template], e.g.
 *
 * ```
 * data class Model(
 *     val name: String
 * ) : BaseModel, ModelDelegate {
 *     override fun delegate(): BaseModel = AppTemplate.SomeTemplate(this)
 * }
 * ```
 *
 * This is only a marker interface, because concrete templates are app specific. The recommendation
 * is to introduce a sealed type with a predefined set of app specific templates, e.g.
 *
 * ```
 * sealed interface AppTemplate : Template {
 *   data class FullScreenTemplate(
 *     val model: BaseModel
 *   ) : AppTemplate
 *
 *   data class ListDetailTemplate(
 *     val listModel: BaseModel,
 *     val detailModel: BaseModel,
 *   ) : AppTemplate
 * }
 * ```
 */
public interface Template : BaseModel

/**
 * Converts any [BaseModel] to a [Template]. If the given model implements [ModelDelegate] and
 * returns a [Template], then this template is used. Otherwise the result of [defaultTemplate] is
 * used.
 */
public inline fun <reified T : Template> BaseModel.toTemplate(
  defaultTemplate: (BaseModel) -> T
): T {
  var model = this
  while (model is ModelDelegate) {
    val delegatedModel = model.delegate()
    if (delegatedModel === model) {
      break
    }
    model = delegatedModel
  }

  return if (model is T) {
    model
  } else {
    defaultTemplate(model)
  }
}
