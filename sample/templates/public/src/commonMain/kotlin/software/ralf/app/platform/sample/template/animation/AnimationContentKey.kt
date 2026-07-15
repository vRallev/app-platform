package software.ralf.app.platform.sample.template.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import software.ralf.app.platform.presenter.BaseModel

/**
 * The sample application supports animations between models and templates. [BaseModel] classes can
 * implement this interface to indicate when a change occurred that should be animated. [contentKey]
 * represents a unique value for an animation state. If the value doesn't change between new models,
 * then no animation will be started.
 *
 * An example may look like this:
 * ```
 * data class Model(
 *     val showPictureFullscreen: Boolean,
 *     ...
 * ) : BaseModel, AnimationContentKey {
 *     override val contentKey: Int =
 *         if (showPictureFullscreen) 1 else AnimationContentKey.DEFAULT_CONTENT_KEY
 * }
 * ```
 *
 * In this sample when `showPictureFullscreen` changes from `true` to `false` and vice versa then an
 * animation will be started using [AnimatedContent]. Use [LocalAnimatedVisibilityScope] and
 * [LocalSharedTransitionScope] to get access to the right scopes.
 */
interface AnimationContentKey {
  /**
   * [contentKey] represents a unique value for an animation state. See [AnimatedContent] for more
   * details.
   */
  val contentKey: Int

  companion object {
    /**
     * The default value for [AnimationContentKey.contentKey], highlighting that no animation should
     * occur.
     */
    const val DEFAULT_CONTENT_KEY = 0

    /**
     * Return [AnimationContentKey.contentKey] for any [BaseModel] instance no matter whether the
     * [AnimationContentKey] was implemented.
     */
    val BaseModel.contentKey: Int
      get() = (this as? AnimationContentKey)?.contentKey ?: DEFAULT_CONTENT_KEY
  }
}

/**
 * All UI composable functions for renderers in the sample application are wrapped within a
 * [AnimatedContent]. This composition local gives access to this wrapper instance to run a shared
 * element transition. For more information see the the
 * [shared element transition documentation](https://developer.android.com/develop/ui/compose/animation/shared-elements#shared-bounds).
 *
 * The [BaseModel] must implement [AnimationContentKey] to indicate that an animation should be
 * played. See [AnimationContentKey] for more details.
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * All UI composable functions for renderers in the sample application are wrapped within a
 * [SharedTransitionLayout]. This composition local gives access to this wrapper instance to run a
 * shared element transition. For more information see the the
 * [shared element transition documentation](https://developer.android.com/develop/ui/compose/animation/shared-elements#shared-bounds).
 *
 * The [BaseModel] must implement [AnimationContentKey] to indicate that an animation should be
 * played. See [AnimationContentKey] for more details.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
