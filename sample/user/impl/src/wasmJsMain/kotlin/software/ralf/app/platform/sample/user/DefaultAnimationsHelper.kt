package software.ralf.app.platform.sample.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

/**
 * Default implementation of [AnimationHelper] that always keeps animations enabled. This
 * implementation is used for Wasm.
 */
@ContributesBinding(AppScope::class)
class DefaultAnimationsHelper : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean = true
}
