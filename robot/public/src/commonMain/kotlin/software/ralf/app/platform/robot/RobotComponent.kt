package software.ralf.app.platform.robot

import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/** Component that provides all contributed [Robot] instances from the dependency graph. */
@ContributesTo(AppScope::class)
public interface RobotComponent {
  /** All [Robot]s provided in the dependency graph. */
  public val robots: Map<KClass<out Robot>, () -> Robot>
}
