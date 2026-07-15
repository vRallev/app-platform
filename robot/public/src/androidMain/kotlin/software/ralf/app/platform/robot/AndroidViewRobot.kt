package software.ralf.app.platform.robot

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.ViewInteraction
import org.hamcrest.Matcher
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.ralf.app.platform.robot.internal.rootScope
import software.ralf.app.platform.scope.di.kotlinInjectComponent

/**
 * A [Robot] specific to interacting with the Android View system.
 *
 * [rootMatcher] allows you to change the root for Espresso assertions, e.g. this is needed when
 * interacting with other windows such as dialogs or in a dual screen setup to change the display
 * for particular robots and make assertions on the second display. By default, Espresso's default
 * root matcher is used.
 *
 * Note that the root matcher needs to be set manually using [ViewInteraction.inRoot]. [onView] is a
 * convenience function that sets the root matcher automatically. It's strongly recommended to use
 * this function instead of [Espresso.onView].
 */
public abstract class AndroidViewRobot : Robot {

  /**
   * The root matcher for this particular robot. It allows you to change the root for Espresso
   * assertions, e.g. this is needed for other windows such as dialogs.
   */
  protected open val rootMatcher: Matcher<Root>
    get() = rootScope.kotlinInjectComponent<Component>().rootMatcherProvider.rootMatcher

  /**
   * Convenience function that automatically sets [rootMatcher] as root for the assertion. It's
   * strongly recommended to use this function instead of [Espresso.onView].
   */
  protected fun onView(viewMatcher: Matcher<View>): ViewInteraction {
    return Espresso.onView(viewMatcher).inRoot(rootMatcher)
  }

  /** Provides objects from the kotlin-inject object graph. */
  @ContributesTo(AppScope::class)
  public interface Component {
    /** The implementation that provides the root matcher. */
    public val rootMatcherProvider: RootMatcherProvider
  }
}
