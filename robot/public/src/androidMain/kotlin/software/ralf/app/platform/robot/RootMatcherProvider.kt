package software.ralf.app.platform.robot

import androidx.test.espresso.Root
import org.hamcrest.Matcher

/**
 * API that provides the default root matcher for Espresso assertions for all [AndroidViewRobot]s.
 * Individual [AndroidViewRobot]s can override the root matcher for their assertions.
 */
public interface RootMatcherProvider {
  /** The default root matcher for all Espresso assertions. */
  public val rootMatcher: Matcher<Root>
}
