package software.ralf.app.platform.template

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraphFactory

/** The main function to launch the Desktop app. */
fun main() {
  val desktopApp = DesktopApp { createGraphFactory<DesktopAppGraph.Factory>().create(it) }

  application {
    Window(
      onCloseRequest = {
        desktopApp.destroy()
        exitApplication()
      },
      alwaysOnTop = true,
      title = "Template App",
    ) {
      desktopApp.renderTemplates()
    }
  }
}
