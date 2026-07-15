package software.ralf.app.platform.sample

import dev.zacsweers.metro.createGraphFactory

/**
 * Application class that is used in instrumented tests. Note that it provides a
 * [TestAndroidApplication] instead of [AndroidApplication].
 */
class TestAndroidApplication : AndroidApplication() {
  override fun metroGraph(demoApplication: DemoApplication): AppGraph {
    return createGraphFactory<TestAndroidAppGraph.Factory>().create(this, demoApplication)
  }
}
