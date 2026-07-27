package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

/** Graph access needed by the iOS view controller. */
@ContributesTo(AppScope::class)
interface IosMainViewControllerGraph {
  /** Factory used to create the view controller's independently cancellable template stream. */
  val templateProviderFactory: TemplateProvider.Factory
}
