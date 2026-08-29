package software.ralf.circuit.listdetail

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** Launches the browser application inside the document body. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  ComposeViewport(checkNotNull(document.body)) {
    val wasmJsApp = remember { WasmJsApp() }
    wasmJsApp.Content()
  }
}
