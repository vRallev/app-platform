package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.createGraphFactory

/** Android test application that installs the graph containing integration-test robots. */
class TestAndroidApplication : AndroidApplication() {
  override fun metroGraph(
    listDetailApplication: software.ralf.app.platform.listdetail.Application
  ): AppGraph {
    return createGraphFactory<TestAndroidAppGraph.Factory>().create(this, listDetailApplication)
  }
}
