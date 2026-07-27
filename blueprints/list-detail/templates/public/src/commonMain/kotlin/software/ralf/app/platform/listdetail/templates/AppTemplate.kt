package software.ralf.app.platform.listdetail.templates

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.template.Template
import software.ralf.app.platform.presenter.template.toTemplate

/**
 * Application-level templates understood by platform entrypoints.
 *
 * Templates provide the stable outer rendering layer around feature-specific models.
 */
sealed interface AppTemplate : Template {
  /** Template that delegates the entire available window to one feature model. */
  data class FullScreenTemplate(
    /** Feature model resolved through the platform's renderer factory. */
    val model: BaseModel
  ) : AppTemplate
}

/** Wraps any feature model in the appropriate [AppTemplate]. */
fun BaseModel.toAppTemplate(): AppTemplate {
  return toTemplate<AppTemplate> { AppTemplate.FullScreenTemplate(it) }
}
