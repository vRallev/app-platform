package software.ralf.app.platform.sample

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import software.ralf.app.platform.sample.user.AnimationHelper
import software.ralf.app.platform.sample.user.DefaultAnimationsHelper

/**
 * This implementation replaces [DefaultAnimationsHelper] in UI tests to disable animations and make
 * tests more stable.
 */
@ContributesBinding(AppScope::class, replaces = [DefaultAnimationsHelper::class])
class TestAnimationHelper : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean = false
}
