package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import software.ralf.app.platform.presenter.BaseModel

/**
 * A renderer that allows you to embed a [BaseAndroidViewRenderer] within a [ComposeRenderer]. All
 * [renderCompose] calls are forwarded to the [androidRenderer]. The Android View hierarchy is
 * embedded within the Compose hierarchy.
 */
internal class AndroidViewWithinComposeRenderer<in ModelT : BaseModel>(
  private val androidRenderer: BaseAndroidViewRenderer<ModelT>
) : BaseAndroidViewRenderer<ModelT>, BaseComposeRenderer<ModelT> {

  private var composeContainer: ViewGroup? = null

  override fun init(activity: Activity, parent: ViewGroup) {
    // Only use composeContainer when it's needed and initialize it lazily. This is needed
    // to embed one ViewRenderer in another where Compose UI is not involved. Then we always
    // need to use the parent passed in here. But once Compose is really used, then use the
    // container from the Compose view and not whatever is passed in here.
    androidRenderer.init(activity, composeContainer ?: parent)
  }

  override fun render(model: ModelT) {
    try {
      androidRenderer.render(model)
    } catch (e: UninitializedPropertyAccessException) {
      throw IllegalStateException(
        "Tried to call render() on an AndroidViewRenderer without a parent view. This usually " +
          "only happens when an AndroidViewRenderer is embedded in Compose UI. Call " +
          "renderCompose() instead.",
        e,
      )
    }
  }

  @Suppress("ComposableNaming")
  @Composable
  override fun renderCompose(model: ModelT, modifier: Modifier) {
    AndroidView(
      factory = { context ->
        // Create a FrameLayout as parent. It's technically not needed, but the
        // API from BaseAndroidViewRenderer requires a parent.
        FrameLayout(context).also {
          composeContainer = it
          init(context as Activity, it)
        }
      }
    ) {
      render(model)
    }
  }
}
