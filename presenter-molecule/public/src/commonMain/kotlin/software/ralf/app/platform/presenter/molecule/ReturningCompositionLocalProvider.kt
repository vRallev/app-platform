package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer
import software.ralf.app.platform.presenter.BaseModel

/**
 * Similar to `CompositionLocalProvider` offered by the Compose runtime itself, but allows us to
 * return a result rather than returning `Unit`.
 */
@Composable
@OptIn(InternalComposeApi::class)
@Suppress("FunctionNaming")
public fun <ModelT : BaseModel> returningCompositionLocalProvider(
  vararg values: ProvidedValue<*>,
  content: @Composable () -> ModelT,
): ModelT {
  currentComposer.startProviders(values)
  val model = content()
  currentComposer.endProviders()
  return model
}
