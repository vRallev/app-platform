package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.ViewGroup
import software.ralf.app.platform.presenter.BaseModel

/** Base type for [Renderer]s that leverage the Android View system. */
public interface BaseAndroidViewRenderer<in ModelT : BaseModel> : Renderer<ModelT> {

  /**
   * Initialize this [Renderer] with an [Activity] and a parent [ViewGroup], in which the [Renderer]
   * should add its views as children.
   */
  public fun init(activity: Activity, parent: ViewGroup)
}
