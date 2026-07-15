package software.ralf.app.platform.robot

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import software.ralf.app.platform.scope.Scope

/**
 * A [Robot] that has access to a [SemanticsNodeInteractionsProvider] and allows you to make
 * assertions and invoke actions on Compose UI elements.
 */
public abstract class ComposeRobot : Robot {

  @PublishedApi internal lateinit var interactionsProvider: SemanticsNodeInteractionsProvider

  /** The [SemanticsNodeInteractionsProvider] to use and interact with Compose UI elements. */
  protected val compose: SemanticsNodeInteractionsProvider
    get() = interactionsProvider
}

/** Creates a [ComposeRobot] of type [T] and invokes the lambda on the newly created robot. */
public inline fun <reified T : Robot> ComposeInteractionsProvider.composeRobot(
  rootScope: Scope = software.ralf.app.platform.robot.internal.rootScope,
  noinline block: T.() -> Unit,
): Unit = semanticsNodeInteractionsProvider.composeRobot<T>(rootScope, block)

/** Creates a [ComposeRobot] of type [T] and invokes the lambda on the newly created robot. */
public inline fun <reified T : Robot> SemanticsNodeInteractionsProvider.composeRobot(
  rootScope: Scope = software.ralf.app.platform.robot.internal.rootScope,
  noinline block: T.() -> Unit,
) {
  robot<T>(rootScope) {
    if (this is ComposeRobot) {
      this.interactionsProvider = this@composeRobot
    }

    block(this)
  }
}
