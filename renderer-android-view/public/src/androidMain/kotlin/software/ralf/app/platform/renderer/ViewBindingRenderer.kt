package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import software.ralf.app.platform.presenter.BaseModel

/**
 * An implementation of [Renderer] that is specific to Android View based UI and uses Android
 * ViewBinding. This renderer provides some built in convenience for rendering Android Views such as
 * managing your child's UI in relation to the provided parent's lifecycle and inflating
 * ViewBindings.
 *
 * Your custom renderers should extend this abstract class and provide implementations for
 * [inflateViewBinding] and [renderModel].
 *
 * [inflateViewBinding] is where one time ViewBinding inflation should take place. This method will
 * not be called again while the Renderer is in use. This method is roughly equivalent to
 * `onCreateView`.
 *
 * ```
 *  override fun inflateViewBinding(
 *      activity: Activity,
 *      parent: ViewGroup,
 *      layoutInflater: LayoutInflater
 *  ): MyLayoutBinding = MyLayoutBinding.inflate(layoutInflater, parent, false)
 * ```
 *
 * [renderModel] is called each time there is a new model for this renderer to render. Place logic
 * here that is specific to binding model data or callbacks to your view layer.
 *
 * ```
 * override fun renderModel(model: MyModel) {
 *     binding.myTextView.text = model.text
 * }
 * ```
 */
public abstract class ViewBindingRenderer<ModelT : BaseModel, ViewBindingT : ViewBinding> :
  ViewRenderer<ModelT>() {

  protected lateinit var binding: ViewBindingT
    private set

  /** Inflates the [ViewBindingT]. */
  final override fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: ModelT,
  ): View {
    binding = inflateViewBinding(activity, parent, layoutInflater, initialModel)
    return binding.root
  }

  /**
   * Provides a hook to inflate [ViewBindingT] and do any other one time set up work for the
   * [Renderer]. Also provides the [initialModel] to enable any needed callbacks to be done.
   */
  protected abstract fun inflateViewBinding(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: ModelT,
  ): ViewBindingT
}
