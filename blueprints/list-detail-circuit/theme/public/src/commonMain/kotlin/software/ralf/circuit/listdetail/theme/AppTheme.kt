package software.ralf.circuit.listdetail.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Theme proxy exposed to feature UIs.
 *
 * Keeping Material 3 access behind this object gives the application a single theme API and avoids
 * coupling individual feature modules directly to global [MaterialTheme] state.
 */
object AppTheme {
  /** Active Material 3 color scheme. */
  val colorScheme: ColorScheme
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

  /** Active Material 3 typography. */
  val typography: Typography
    @Composable @ReadOnlyComposable get() = MaterialTheme.typography

  /** Active Material 3 shape definitions. */
  val shapes: Shapes
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}
