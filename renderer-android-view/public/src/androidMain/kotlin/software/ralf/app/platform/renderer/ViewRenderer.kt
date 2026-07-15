package software.ralf.app.platform.renderer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnDetach
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import software.ralf.app.platform.presenter.BaseModel

/**
 * An implementation of [Renderer] that is specific to Android View based UI. This renderer provides
 * some built in convenience for rendering Android Views such as managing your child's UI in
 * relation to the provided parent's lifecycle.
 *
 * Your custom renderers should extend this abstract class and provide implementations for [inflate]
 * and [renderModel].
 *
 * [inflate] is where one time view set up should take place. This method will not be called again
 * while the Renderer is in use. This method is roughly equivalent to `onCreateView` and should be
 * used inflate layouts or write UI code directly in Kotlin.
 *
 * ```
 *  private lateinit var myView: TextView
 *
 *  override fun inflate(
 *      activity: Activity,
 *      parent: ViewGroup,
 *      layoutInflater: LayoutInflater
 *  ): View {
 *      return layoutInflator.inflate(R.layout.myView, parent, false).also {
 *          // Here you can initialize other variables you need to use in your [renderModel] method.
 *          myView = it.findViewById(R.id.myView)
 *      }
 *  }
 * ```
 *
 * [renderModel] is called each time there is a new model for this renderer to render. Place logic
 * here that is specific to binding model data or callbacks to your view layer.
 *
 * ```
 * override fun renderModel(model: MyModel) {
 *     myView.text = model.text
 * }
 * ```
 *
 * Optionally your custom renderer can override [onDetach] to perform cleanup tasks before your
 * views are removed from the hierarchy. Once the inflated view was detached from the window, it
 * will be removed from the parent and nullified. If this [ViewRenderer] is reused, then a new view
 * will be inflated and added to the parent.
 */
public abstract class ViewRenderer<in ModelT : BaseModel> : BaseAndroidViewRenderer<ModelT> {

  /**
   * The [Activity] that was provided to the [AndroidRendererFactory]. The same [Activity] is passed
   * as argument to [inflate].
   */
  protected lateinit var activity: Activity
    private set

  /**
   * A [CoroutineScope] gets created before the view gets inflated in [inflate]. In can be used to
   * register UI related background work. This [CoroutineScope] gets canceled when the view is
   * removed from the view hierarchy or [activity] gets destroyed.
   */
  protected lateinit var coroutineScope: CoroutineScope
    private set

  private lateinit var parent: ViewGroup

  private var view: View? = null
  private var lastModel: ModelT? = null

  private val onAttachListener =
    object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        // Invoke this callback only once.
        v.removeOnAttachStateChangeListener(this)

        onViewAttached(v)
      }

      override fun onViewDetachedFromWindow(v: View) = Unit
    }

  final override fun init(activity: Activity, parent: ViewGroup) {
    if (!this::activity.isInitialized) {
      // Renderer is not initialized.
      this.activity = activity
      this.parent = parent
    }

    // We don't want to allow changing the parent. However, during the initialization procedure
    // we call init() with the default parent from the RendererFactory and eventually init()
    // again with the parent provided getRenderer() call. If no view has been created yet and the
    // parent hasn't been used yet, then it is okay to update the value.

    if (view == null) {
      this.parent = parent
    }

    check(this.activity == activity && this.parent == parent) {
      "A ViewRenderer should ever be only attached to one parent view. Current parent is " +
        "${this.parent}, new parent is $parent"
    }
  }

  private fun createView(model: ModelT): View {
    val mainCoroutineScope = MainScope().also { coroutineScope = it }

    activity.doOnDestroy { mainCoroutineScope.cancel() }

    return inflate(activity, parent, activity.layoutInflater, model).also { view = it }
  }

  private fun onViewAttached(view: View) {
    lastModel = null

    // Wait for the view to be attached first, otherwise doOnDetach gets
    // called immediately.
    view.doOnDetach {
      if (releaseViewOnDetach()) {
        // call onDetach first so view is still available during cleanup tasks
        onDetach()
        resetView(view)
      }
    }
  }

  private fun resetView(view: View) {
    coroutineScope.cancel()

    // Allows us to reclaim the memory. Reset all cached value before calling removeView() in case
    // there are any recursive calls with doOnDetach for child views.
    this.view = null

    // Reset the last model, because we want to re-render when the view is attached again even
    // for the same model.
    lastModel = null

    // Remove the view from the parent. In case the Renderer is reused we inflate a new
    // View and add it to the parent. This action must run after all currently queued main
    // thread operations finish. This is needed because we cannot remove views from parents in
    // the onDetach callback as this may lead to inconsistent view state and trigger crashes.
    Handler(Looper.getMainLooper()).post {
      if (view.parent === parent) {
        parent.removeView(view)
      }
    }
  }

  final override fun render(model: ModelT) {
    val view = view ?: createView(model)

    if (parent.children.none { it === view }) {
      parent.addView(view)

      // In case we registered the callback before, remove it first. There is no API to check
      // whether this callback has been registered before. The callback should only be registered
      // once.
      view.removeOnAttachStateChangeListener(onAttachListener)

      // This implementation below is similar to `doOnAttach {}`, which we used to use. However,
      // this extension function didn't allow us to unregister the previous callback and we
      // accidentally registered too many. That's why we had to extract `onAttachListener` into
      // a variable.
      if (view.isAttachedToWindow) {
        onViewAttached(view)
      } else {
        view.addOnAttachStateChangeListener(onAttachListener)
      }
    }

    // Avoid re-rendering same model
    if (model == lastModel) {
      return
    }

    renderModel(model, lastModel)
    lastModel = model
  }

  /**
   * Perform one time view inflation for the layout of this Renderer. This method is called on
   * initial Render and won't be called again; it is therefore a good place to do initial setup that
   * need only be done a single time.
   */
  protected abstract fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: ModelT,
  ): View

  /**
   * Called when a new Model is given to the Renderer, use if there is no need to compare [model]
   * with any prior model in order to render specific views. This function will not be called if
   * `renderModel(model: T, lastModel: T?)` is overridden.
   */
  protected open fun renderModel(model: ModelT): Unit = Unit

  /**
   * Called when a new Model is given to the Renderer. Provides an opportunity for a Renderer to
   * compare [model] with [lastModel]. By default, this calls `renderModel(model: T)`.
   */
  protected open fun renderModel(model: ModelT, lastModel: ModelT?): Unit = renderModel(model)

  /**
   * Inheritors of this class can optionally override this method to do cleanup tasks before their
   * views are removed from the hierarchy.
   */
  public open fun onDetach(): Unit = Unit

  /**
   * This function is called when the view inflated by this renderer was detached, meaning the view
   * itself or one of its parents got removed from the view hierarchy.
   *
   * When this function returns `true`, then the view is manually removed from the parent and
   * released, meaning it can be garbage collected. [coroutineScope] is canceled at the same time.
   * The next time [render] is called, [inflate] will be called, a new view gets inflated and added
   * to the parent.
   *
   * When this function returns `false`, then the view is cached and will not be removed from the
   * parent. [coroutineScope] isn't canceled either. This behavior is usually desired in view groups
   * that manually manage their children, such as a [RecyclerView].
   *
   * The default implementation of this function checks whether any of the parent views is an
   * instance of [RecyclerView] and returns the appropriate result. This behavior can be changed by
   * subclasses of [ViewRenderer].
   */
  protected open fun releaseViewOnDetach(): Boolean {
    val parents =
      generateSequence(parent) { viewGroup -> viewGroup.parent as? ViewGroup }
        .takeWhile { it.id != android.R.id.content }

    return parents.none { it is RecyclerView }
  }

  private companion object {
    fun Activity.doOnDestroy(block: (Activity) -> Unit) {
      if (isDestroyed) {
        block(this)
        return
      }

      application.registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
          override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

          override fun onActivityStarted(activity: Activity) = Unit

          override fun onActivityResumed(activity: Activity) = Unit

          override fun onActivityPaused(activity: Activity) = Unit

          override fun onActivityStopped(activity: Activity) = Unit

          override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

          override fun onActivityDestroyed(activity: Activity) {
            if (activity == this@doOnDestroy) {
              application.unregisterActivityLifecycleCallbacks(this)
              block(this@doOnDestroy)
            }
          }
        }
      )
    }
  }
}
