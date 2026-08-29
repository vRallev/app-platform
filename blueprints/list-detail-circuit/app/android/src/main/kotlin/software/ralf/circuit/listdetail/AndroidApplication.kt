package software.ralf.circuit.listdetail

import android.app.Application
import dev.zacsweers.metro.createGraph

class AndroidApplication : Application(), AppGraphProvider {
  override lateinit var appGraph: AppGraph
    private set

  override fun onCreate() {
    super.onCreate()
    appGraph = createGraph<AndroidAppGraph>()
  }
}
