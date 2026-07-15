package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.flow.MutableStateFlow
import software.ralf.app.platform.presenter.BaseModel

/**
 * A renderer that allows you to embed Compose UI within an Android View hierarchy. All [render]
 * calls are forwarded to the [Compose] function using a flow.
 *
 * This class is abstract to allow for different Compose UI implementations, but it's not exposed to
 * consumers yet.
 */
internal abstract class BaseComposeWithinAndroidViewRenderer<in ModelT : BaseModel> :
  ViewRenderer<ModelT>(), BaseComposeRenderer<ModelT> {

  private val models = MutableStateFlow<ModelT?>(null)

  final override fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: ModelT,
  ): View =
    ComposeView(activity).apply {
      setContent {
        val model = models.collectAsState().value
        if (model != null) {
          // Render the new model using Compose.
          renderCompose(model)
        }
      }
    }

  // Make these functions final. Concrete renderers should only operate in the Compose world.
  final override fun onDetach() {
    // Clear any references to the last model or presenter.
    models.value = null
  }

  @Suppress("ComposableNaming")
  @Composable
  final override fun renderCompose(model: ModelT, modifier: Modifier) {
    Compose(model)
  }

  final override fun renderModel(model: ModelT): Nothing = throw NotImplementedError()

  final override fun renderModel(model: ModelT, lastModel: ModelT?) {
    models.value = model
  }

  /** Render the given [model] on screen using Compose UI. */
  @Composable protected abstract fun Compose(model: ModelT)
}
