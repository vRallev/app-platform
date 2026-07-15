package software.ralf.app.platform.presenter.template

import software.ralf.app.platform.presenter.BaseModel

/**
 * Can be implemented by a [BaseModel] class to delegate to another [BaseModel]. This is helpful for
 * presenters whose only concern is navigation to say: render this model or render that model
 * instead, e.g.
 *
 * ```
 * data class NavigationModel(
 *     private val model1: BaseModel,
 *     private val model2: BaseModel,
 *     private val condition: Boolean,
 * ) : BaseModel, ModelDelegate {
 *     override fun delegate(): BaseModel = if (condition) model1 else model2
 * }
 */
public interface ModelDelegate {
  /** Returns the Model to which this Model should delegate. */
  public fun delegate(): BaseModel
}
