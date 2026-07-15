package software.ralf.app.platform.presenter.molecule.backgesture

import androidx.annotation.FloatRange
import androidx.annotation.IntRange

/**
 * Object used to report back gesture progress. Holds information about the touch event, swipe
 * direction and the animation progress that predictive back animations should seek to.
 */
// Note, this is a copy from
// https://github.com/JetBrains/compose-multiplatform-core/blob/244635e202f9aa734bd8c86bd1748a9065ecd818/compose/ui/ui-backhandler/src/commonMain/kotlin/androidx/compose/ui/backhandler/BackEventCompat.kt
//
// By copying the class we don't need to expose the Compose Multiplatform APIs from the presenter
// artifact, which is independent of the UI layer implementation.
public class BackEventPresenter(
  /**
   * Absolute X location of the touch point of this event in the coordinate space of the view that
   * * received this back event.
   */
  public val touchX: Float,
  /**
   * Absolute Y location of the touch point of this event in the coordinate space of the view that
   * received this back event.
   */
  public val touchY: Float,
  /** Value between 0 and 1 on how far along the back gesture is. */
  @get:FloatRange(from = 0.0, to = 1.0) public val progress: Float,
  /** Indicates which edge the swipe starts from. */
  @get:IntRange(from = 0, to = 1) public val swipeEdge: Int,
) {
  public companion object {
    /** Indicates that the edge swipe starts from the left edge of the screen. */
    public const val EDGE_LEFT: Int = 0

    /** Indicates that the edge swipe starts from the right edge of the screen. */
    public const val EDGE_RIGHT: Int = 1
  }
}
