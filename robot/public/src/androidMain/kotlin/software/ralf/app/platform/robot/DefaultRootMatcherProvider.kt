package software.ralf.app.platform.robot

import androidx.test.espresso.Root
import androidx.test.espresso.matcher.RootMatchers
import me.tatarka.inject.annotations.Inject
import org.hamcrest.Matcher
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** Implementation of [RootMatcherProvider] that provides Espresso's default root matcher. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class DefaultRootMatcherProvider : RootMatcherProvider {
  override val rootMatcher: Matcher<Root>
    get() = RootMatchers.DEFAULT
}
