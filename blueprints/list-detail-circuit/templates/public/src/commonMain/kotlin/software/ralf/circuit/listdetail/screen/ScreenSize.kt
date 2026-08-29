package software.ralf.circuit.listdetail.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Window dimensions and the adaptive category derived from them.
 *
 * Instances are created through [from] so category calculation remains consistent across all
 * platforms.
 */
@ConsistentCopyVisibility
data class ScreenSize
private constructor(
  /** Current window width in density-independent pixels. */
  val width: Dp,
  /** Current window height in density-independent pixels. */
  val height: Dp,
  /** Layout category selected from [width] and [height]. */
  val category: Category,
) {
  /** Adaptive layouts supported by the blueprint. */
  enum class Category {
    /** Single-pane navigation intended for phones and portrait windows. */
    PHONE,

    /** Two-pane layout intended for sufficiently wide landscape windows. */
    TABLET,
  }

  companion object {
    /** Initial value used before a platform reports a specified window size. */
    val Zero = from(width = 0.dp, height = 0.dp)

    /**
     * Creates a screen size and derives its adaptive category.
     *
     * Tablet layout requires both a width of at least 600 dp and landscape orientation.
     */
    fun from(width: Dp, height: Dp): ScreenSize {
      require(width.isSpecified) { "width must be specified." }
      require(height.isSpecified) { "height must be specified." }

      return ScreenSize(
        width = width,
        height = height,
        category =
          if (width >= 600.dp && width > height) {
            Category.TABLET
          } else {
            Category.PHONE
          },
      )
    }
  }
}
