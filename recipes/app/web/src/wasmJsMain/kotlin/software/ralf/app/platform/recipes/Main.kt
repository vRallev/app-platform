package software.ralf.app.platform.recipes

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** The entry point of our recipes app. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  ComposeViewport(checkNotNull(document.body)) {
    val wasmJsApp = remember {
      WasmJsApp { WasmJsAppComponent::class.create(it) }
    }

    DisposableEffect(wasmJsApp) { onDispose { wasmJsApp.destroy() } }
    wasmJsApp.renderTemplates()
  }
}
