package software.ralf.app.platform.listdetail.screen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped implementation of [ScreenSizeProvider].
 *
 * The template renderer writes platform window measurements while feature presenters observe only
 * the read-only [screenSize] flow.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultScreenSizeProvider : ScreenSizeProvider {
  private val mutableScreenSize = MutableStateFlow(ScreenSize.Zero)

  override val screenSize: StateFlow<ScreenSize> = mutableScreenSize.asStateFlow()

  /** Publishes a new platform window measurement to shared presentation code. */
  fun update(screenSize: ScreenSize) {
    mutableScreenSize.value = screenSize
  }
}
