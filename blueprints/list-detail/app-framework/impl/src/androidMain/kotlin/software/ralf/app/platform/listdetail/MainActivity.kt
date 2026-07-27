package software.ralf.app.platform.listdetail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import software.ralf.app.platform.renderer.ComposeAndroidRendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer
import software.ralf.app.platform.scope.RootScopeProvider

/** Android entry point that renders the shared template stream. */
class MainActivity : ComponentActivity() {
  private val rootScopeProvider
    get() = application as RootScopeProvider

  private val viewModel by viewModels<MainActivityViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val rendererFactory =
      ComposeAndroidRendererFactory.createForComposeUi(rootScopeProvider = rootScopeProvider)

    setContent {
      val template by viewModel.templates.collectAsState()
      rendererFactory.getComposeRenderer(template).renderCompose(template)
    }
  }
}
