@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.ralf.circuit.listdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import software.ralf.circuit.listdetail.screen.LocalScreenSize
import software.ralf.circuit.listdetail.screen.ScreenSize

/**
 * Stable identity for one character attribute shared by the list and detail UIs.
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

@Composable
internal fun CharacterSharedElementScope(
  content: @Composable (SharedElementTransitionScope?) -> Unit
) {
  val isPhone = LocalScreenSize.current.category == ScreenSize.Category.PHONE
  if (isPhone && SharedElementTransitionScope.isAvailable) {
    SharedElementTransitionScope { content(this) }
  } else {
    content(null)
  }
}

/**
 * Matches a character attribute with its counterpart during single-pane navigation.
 *
 * The modifier is unchanged when rendered outside a Circuit animated entry or in the tablet split
 * view, keeping shared transitions limited to compact and portrait layouts.
 */
@Composable
internal fun Modifier.sharedCharacterBounds(
  characterId: String,
  element: CharacterSharedElementKey.Element,
  transitionScope: SharedElementTransitionScope?,
): Modifier {
  val animatedVisibilityScope = transitionScope?.findAnimatedScope(Navigation)

  return if (animatedVisibilityScope != null) {
    with(transitionScope) {
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
