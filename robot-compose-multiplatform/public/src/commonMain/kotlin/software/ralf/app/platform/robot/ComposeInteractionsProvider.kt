package software.ralf.app.platform.robot

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * Provides the [SemanticsNodeInteractionsProvider] that is forwarded to [ComposeRobot]s. This
 * interface is usually implemented by the test class and forwards the Compose test rule.
 */
public interface ComposeInteractionsProvider {
  /** Provides access to Compose UI interactions. */
  public val semanticsNodeInteractionsProvider: SemanticsNodeInteractionsProvider
}
