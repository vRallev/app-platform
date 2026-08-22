package software.ralf.app.platform.template

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.createGraphFactory
import kotlinx.browser.document

/** The entry point of our sample app. */
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
