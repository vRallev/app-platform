@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.ralf.app.platform.listdetail.templates

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Animated navigation scope used by renderers participating in a shared element transition.
 *
 * The backstack renderer supplies this from Navigation 3's internal `AnimatedContent`. It remains
 * nullable so renderers can also be hosted outside navigation, including the tablet split view.
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Shared transition scope installed around the application's rendered content.
 *
 * Feature renderers use this scope together with [LocalAnimatedVisibilityScope] to match elements
 * across outgoing and incoming navigation entries.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
