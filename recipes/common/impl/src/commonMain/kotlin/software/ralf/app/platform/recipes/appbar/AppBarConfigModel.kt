package software.ralf.app.platform.recipes.appbar

import software.ralf.app.platform.presenter.BaseModel

/** Can be implemented by a [BaseModel] class to change the configuration of the App Bar. */
interface AppBarConfigModel {
  /** Returns the config that should be rendered. */
  fun appBarConfig(): AppBarConfig
}
