package software.ralf.app.platform.recipes.appbar

/** Configures the look of the App Bar for the recipe app. */
data class AppBarConfig(
  /** The title shown in the center of the App Bar. */
  val title: String,
  /**
   * If not null, then the back arrow will be shown and the lambda invoked when there's an action.
   */
  val backArrowAction: (() -> Unit)? = null,

  /** A list of menu items that should be shown in the overflow menu if any. */
  val menuItems: List<MenuItem> = emptyList(),
) {
  /**
   * An element in the overflow menu with [text] as the title. [action] is invoked when this element
   * is pressed.
   */
  data class MenuItem(val text: String, val action: () -> Unit)

  companion object {
    /** The default configuration used when no presenter overrides the config. */
    val DEFAULT = AppBarConfig(title = "Recipes App")
  }
}
