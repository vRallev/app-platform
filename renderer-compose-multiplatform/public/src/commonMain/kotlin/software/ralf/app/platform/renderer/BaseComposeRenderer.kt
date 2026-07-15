package software.ralf.app.platform.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import software.ralf.app.platform.presenter.BaseModel

/**
 * The base interface for Compose UI enabled renderers. This interface is similar to [Renderer], but
 * does not extend the [Renderer] interface itself. It has its own [renderCompose] function in order
 * to preserve the Compose UI context.
 *
 * This interface will rarely be used directly and a more specific renderer implementation for a
 * concrete platform such as [ComposeRenderer] should be favored unless there is a specific need.
 *
 * For more information see [Renderer].
 */
// For future reviews:
//
// This interface was named "BaseComposeRenderer", because most consumers will
// import "ComposeRenderer" directly and this class name is more user friendly than something
// along the lines of "RealComposeRenderer".
//
// This separate base interface is needed for several implementations that cannot extend the
// abstract ComposeRenderer class. It also helps to distinguish between the normal Renderer API
// vs this BaseComposeRenderer API. Note that BaseComposeRenderer is not extending the Renderer
// interface. This distinction is similar to Presenter and MoleculePresenter.
public interface BaseComposeRenderer<in ModelT : BaseModel> {
  /** Render the given [model] on screen using Compose UI with the provided [modifier]. */
  // Android Lint will complain that this function should start with an uppercase letter, but
  // the name "renderCompose" was chosen to align it with the "render" function from the
  // Renderer interface. Implementations will usually have a separate "Compose()" function, see
  // ComposeRenderer for example.
  @Composable public fun renderCompose(model: ModelT, modifier: Modifier = Modifier)
}
