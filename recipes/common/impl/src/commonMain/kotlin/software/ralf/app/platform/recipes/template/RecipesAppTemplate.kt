package software.ralf.app.platform.recipes.template

import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.template.Template
import software.ralf.app.platform.recipes.appbar.AppBarConfig

/** All [Template]s implemented in the recipes application. */
sealed interface RecipesAppTemplate : Template {
  /** A template that hosts a single model, which should rendered as full-screen element. */
  data class FullScreenTemplate(
    /** The model to be rendered fullscreen. */
    val model: BaseModel,
    /** The configuration for the app bar of the recipe app. */
    val appBarConfig: AppBarConfig,
  ) : RecipesAppTemplate
}
