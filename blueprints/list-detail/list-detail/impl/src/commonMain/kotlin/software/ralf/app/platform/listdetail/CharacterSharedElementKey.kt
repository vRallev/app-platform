@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.ralf.app.platform.listdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import software.ralf.app.platform.listdetail.screen.LocalScreenSize
import software.ralf.app.platform.listdetail.screen.ScreenSize
import software.ralf.app.platform.listdetail.templates.LocalAnimatedVisibilityScope
import software.ralf.app.platform.listdetail.templates.LocalSharedTransitionScope

/**
 * Stable identity for one character attribute shared by the list and detail renderers.
 *
 * Including both [characterId] and [element] prevents different characters or attributes from
 * matching while multiple list rows remain composed during navigation.
 */
internal data class CharacterSharedElementKey(
  /** Character owning the transitioning attribute. */
  val characterId: String,
  /** Attribute participating in the transition. */
  val element: Element,
) {
  /** Character attributes that transition between list and detail content. */
  enum class Element {
    /** Character profile image. */
    Portrait,

    /** Character display name. */
    Name,

    /** Character age when the Ring was destroyed. */
    Age,
  }
}

/**
 * Matches a character attribute with its counterpart during single-pane navigation.
 *
 * The modifier is unchanged when rendered outside a Navigation 3 animated entry or in the tablet
 * split view, keeping shared transitions limited to compact and portrait layouts.
 */
@Composable
internal fun Modifier.sharedCharacterBounds(
  characterId: String,
  element: CharacterSharedElementKey.Element,
): Modifier {
  val isPhone = LocalScreenSize.current.category == ScreenSize.Category.PHONE
  val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
  val sharedTransitionScope = LocalSharedTransitionScope.current

  return if (isPhone && animatedVisibilityScope != null && sharedTransitionScope != null) {
    with(sharedTransitionScope) {
      sharedBounds(
        sharedContentState =
          rememberSharedContentState(
            key = CharacterSharedElementKey(characterId = characterId, element = element)
          ),
        animatedVisibilityScope = animatedVisibilityScope,
      )
    }
  } else {
    this
  }
}
