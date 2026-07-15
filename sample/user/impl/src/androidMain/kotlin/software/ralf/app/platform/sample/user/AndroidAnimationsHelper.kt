package software.ralf.app.platform.sample.user

import android.app.Application
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

/**
 * Android implementation of [AnimationHelper] which queries the device state to determine whether
 * animations are enabled.
 */
@ContributesBinding(AppScope::class)
class AndroidAnimationsHelper(private val application: Application) : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean {
    val duration =
      Settings.Global.getFloat(
        application.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
      )

    return duration > 0f
  }
}
