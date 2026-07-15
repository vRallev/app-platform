package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import software.ralf.app.platform.presenter.BaseModel

/**
 * A specific [Renderer] implementation that can be used for single elements in a [RecyclerView].
 * This implementation is different than [ViewRenderer] in the way that views, which are
 * instantiated by [inflate], aren't removed from the view hierarchy nor nullified when they get
 * detached. It's expected in a [RecyclerView] that views frequently are detached and attached
 * (recycled).
 *
 * An implementation could look like this:
 * ```
 * @ContributesRenderer
 * class SampleRowRenderer : RecyclerViewHolderRenderer<SampleRowPresenter.Model>() {
 *
 *     private lateinit var textView: TextView
 *
 *     override fun inflate(
 *         activity: Activity,
 *         parent: ViewGroup,
 *         layoutInflater: LayoutInflater,
 *     ): View {
 *         return layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false).also {
 *             textView = it.findViewById(android.R.id.text1)
 *         }
 *     }
 *
 *     override fun render(model: SampleRowPresenter.Model) {
 *         textView.text = model.title
 *     }
 * }
 * ```
 *
 * The expected pattern is to use [RendererFactory] in a [RecyclerView.Adapter] to create instances
 * of [RecyclerViewViewHolderRenderer] and call them whenever a view needs to be updated:
 * ```
 * private class SampleAdapter(
 *     private val rendererFactory: RendererFactory,
 * ) : RecyclerView.Adapter<RecyclerViewHolderRenderer.ViewHolder<SampleRowPresenter.Model>>() {
 *
 *     override fun onCreateViewHolder(
 *         parent: ViewGroup,
 *         viewType: Int,
 *     ): RecyclerViewHolderRenderer.ViewHolder<SampleRowPresenter.Model> {
 *         val renderer = rendererFactory
 *             .createRenderer(SampleRowPresenter.Model::class, parent) as RecyclerViewHolderRenderer
 *
 *         return renderer.viewHolder()
 *     }
 *
 *     override fun onBindViewHolder(
 *         holder: RecyclerViewHolderRenderer.ViewHolder<SampleRowPresenter.Model>,
 *         position: Int,
 *     ) {
 *         val sampleRowModel = currentModel(position)
 *
 *         holder.renderer.render(sampleRowModel.model)
 *     }
 * }
 * ```
 *
 * [viewHolder] provides the [RecyclerView.ViewHolder] instance, which keeps a reference to the
 * [RecyclerViewViewHolderRenderer].
 */
public abstract class RecyclerViewViewHolderRenderer<ModelT : BaseModel> :
  BaseAndroidViewRenderer<ModelT> {

  private var _activity: Activity? = null
  private var _parent: ViewGroup? = null

  /** The Android [Activity] associated with this [Renderer]. This value will never change. */
  protected val activity: Activity
    get() = checkNotNull(_activity) { "Call init() first." }

  private val parent: ViewGroup
    get() = checkNotNull(_parent) { "Call init() first." }

  private var viewHolder: ViewHolder<ModelT>? = null

  final override fun init(activity: Activity, parent: ViewGroup) {
    if (_activity == null) {
      _activity = activity
      _parent = parent
    } else if (activity != this.activity || parent != this.parent) {
      // This is because the ViewHolder is associated with a single RecyclerView. The
      // memory will be reclaimed when the RecyclerView is removed from the view hierarchy.
      @Suppress("UseCheckOrError")
      throw IllegalStateException(
        "A RecyclerViewViewHolderRenderer should only be reused with the same parent view."
      )
    }
  }

  /**
   * The [RecyclerView.ViewHolder] associated with this [RecyclerViewViewHolderRenderer]. This
   * `ViewHolder` should be returned from [RecyclerView.Adapter.onCreateViewHolder]:
   * ```
   * override fun onCreateViewHolder(
   *     parent: ViewGroup,
   *     viewType: Int,
   * ): RecyclerViewHolderRenderer.ViewHolder<SampleRowPresenter.Model> {
   *     val renderer = rendererFactory
   *         .createRenderer(SampleRowPresenter.Model::class, parent) as RecyclerViewHolderRenderer
   *
   *     return renderer.viewHolder()
   * }
   * ```
   */
  public fun viewHolder(): ViewHolder<ModelT> {
    return viewHolder
      ?: ViewHolder(inflate(activity, parent, activity.layoutInflater), this).also {
        viewHolder = it
      }
  }

  /**
   * Perform one time view inflation for the layout of this `Renderer`. This method is called when
   * the [viewHolder] is initialized and won't be called again. It is therefore a good place to do
   * initial setup that need only be done a single time.
   */
  protected abstract fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
  ): View

  /**
   * Common [RecyclerView.ViewHolder] implementation that provides access to your particular
   * [renderer] instance. Retrieve the [renderer] in [RecyclerView.Adapter.onBindViewHolder] and
   * call [Renderer.render]:
   * ```
   * override fun onBindViewHolder(
   *     holder: RecyclerViewHolderRenderer.ViewHolder<SampleRowPresenter.Model>,
   *     position: Int,
   * ) {
   *     val sampleRowModel = currentModel(position)
   *
   *     holder.renderer.render(sampleRowModel.model)
   * }
   * ```
   */
  public class ViewHolder<M : BaseModel>
  internal constructor(view: View, public val renderer: Renderer<M>) : RecyclerView.ViewHolder(view)
}
