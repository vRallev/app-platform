package software.ralf.app.platform.robot

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import kotlin.reflect.KClass

/** Graph that provides all contributed [Robot] instances from the Metro dependency graph. */
@ContributesTo(AppScope::class)
public interface RobotGraph {
  /** All [Robot]s provided in the Metro dependency graph. */
  public val robots: Map<KClass<*>, () -> Robot>

  /**
   * Allows graphs to expose an empty map when no robots are contributed. App-scope graphs include
   * this container automatically. Other graphs implementing [RobotGraph] can include it through
   * `@DependencyGraph(bindingContainers = [RobotGraph.Bindings::class])`.
   */
  @ContributesTo(AppScope::class)
  @BindingContainer
  public interface Bindings {
    /** Declares the robot factory map without requiring any contributions. */
    @Multibinds(allowEmpty = true) public val robots: Map<KClass<*>, () -> Robot>
  }
}
