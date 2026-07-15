package software.ralf.app.platform.sample.template

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.template.Template
import software.ralf.app.platform.sample.template.animation.AnimationContentKey
import software.ralf.app.platform.sample.template.animation.AnimationContentKey.Companion.contentKey

/** All [Template]s implemented in the sample application. */
sealed interface SampleAppTemplate : Template, AnimationContentKey {
  /** A template that hosts a single model, which should rendered as full-screen element. */
  data class FullScreenTemplate(
    /** The model to be rendered fullscreen. */
    val model: BaseModel
  ) : SampleAppTemplate {
    override val contentKey: Int
      get() = model.contentKey
  }

  /**
   * A template that hosts two models, these can be rendered in different configurations, at the
   * discretion of the [Template]'s `Renderer`. These two models are meant to be related to each
   * other through the list model's selection state, which influences the data in the detail model.
   */
  data class ListDetailTemplate(
    /**
     * The list model. Typically rendered on less screen real estate and is meant to be used to show
     * a high level overview of some data.
     */
    val list: BaseModel,

    /**
     * The detail model. Typically rendered on more screen real estate than the list model and is
     * meant to be used to show more detailed information.
     */
    val detail: BaseModel,
  ) : SampleAppTemplate {
    override val contentKey: Int
      // Multiply by 31 to avoid collisions in the sum, e.g. when list changes from 0 to 1 and
      // detail changes from 1 to 0 at teh same time.
      get() = list.contentKey * 31 + detail.contentKey
  }
}
