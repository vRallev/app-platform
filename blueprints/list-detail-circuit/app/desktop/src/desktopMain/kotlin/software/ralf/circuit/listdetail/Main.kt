package software.ralf.circuit.listdetail

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Taskbar
import javax.imageio.ImageIO

private const val WINDOW_ICON_RESOURCE = "app-icon.png"

/** Launches the Desktop application. Press Command/Ctrl+S to toggle phone and tablet sizes. */
@Suppress("DEPRECATION")
fun main() {
  setTaskbarIcon()

  val desktopApp = DesktopApp()

  application {
    val phoneSize = DesktopWindowSizes.phone
    val tabletSize = DesktopWindowSizes.tablet
    val windowState = rememberWindowState(width = phoneSize.width, height = phoneSize.height)

    Window(
      onCloseRequest = {
        exitApplication()
      },
      icon = painterResource(WINDOW_ICON_RESOURCE),
      onPreviewKeyEvent = { keyEvent ->
        if (
          keyEvent.type == KeyEventType.KeyDown &&
            (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) &&
            keyEvent.key == Key.S
        ) {
          windowState.size =
            if (DesktopWindowSizes.isTablet(windowState.size)) phoneSize else tabletSize
          true
        } else {
          false
        }
      },
      state = windowState,
      title = "Middle-earth · Circuit",
    ) {
      desktopApp.Content()
    }
  }
}

/**
 * Sets the icon for unpackaged desktop processes such as Compose Hot Reload.
 *
 * Native distributions use their platform-specific icon files, but Hot Reload launches the JVM
 * process directly and therefore needs to update the operating system taskbar explicitly.
 */
private fun setTaskbarIcon() {
  runCatching {
    if (!Taskbar.isTaskbarSupported()) return

    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return

    val iconUrl =
      Thread.currentThread().contextClassLoader.getResource(WINDOW_ICON_RESOURCE) ?: return
    taskbar.iconImage = ImageIO.read(iconUrl)
  }
}
