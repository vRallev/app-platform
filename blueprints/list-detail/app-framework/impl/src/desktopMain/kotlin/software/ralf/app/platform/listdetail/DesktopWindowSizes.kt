package software.ralf.app.platform.listdetail

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Useful window presets for exercising compact and two-pane layouts. */
object DesktopWindowSizes {
  /** Portrait preset that exercises the phone backstack presentation. */
  val phone = DpSize(width = 480.dp, height = 840.dp)

  /** Landscape preset that exercises the tablet two-pane presentation. */
  val tablet = DpSize(width = 1100.dp, height = 760.dp)

  /** Returns whether [size] satisfies the same two-pane rule as shared presentation code. */
  fun isTablet(size: DpSize): Boolean {
    return size.width >= 600.dp && size.width > size.height
  }
}
