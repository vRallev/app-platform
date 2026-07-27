package software.ralf.app.platform.listdetail

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.createGraphFactory
import kotlinx.browser.document

/** Launches the browser application inside the document body. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  ComposeViewport(checkNotNull(document.body)) {
    val wasmJsApp = remember {
      WasmJsApp { createGraphFactory<WasmJsAppGraph.Factory>().create(it) }
    }

    DisposableEffect(wasmJsApp) { onDispose { wasmJsApp.destroy() } }
    wasmJsApp.renderTemplates()
  }
}
