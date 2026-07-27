package software.ralf.app.platform.listdetail.screen

import kotlinx.coroutines.flow.StateFlow

/** Observable boundary between platform window measurement and shared presentation code. */
interface ScreenSizeProvider {
  /** Most recently reported window size and adaptive category. */
  val screenSize: StateFlow<ScreenSize>
}
