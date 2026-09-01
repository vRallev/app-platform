package software.ralf.app.platform.internal

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

actual fun createMarkedDispatcher(): MarkedDispatcher = JvmMarkedDispatcher()

private class JvmMarkedDispatcher : MarkedDispatcher {
  private val currentThreadMarker = ThreadLocal<Boolean>()
  private val executor =
    Executors.newSingleThreadExecutor { runnable ->
      Thread {
        currentThreadMarker.set(true)
        try {
          runnable.run()
        } finally {
          currentThreadMarker.remove()
        }
      }
    }
  private val closeableDispatcher = executor.asCoroutineDispatcher()

  override val dispatcher: CoroutineDispatcher = closeableDispatcher

  override fun isCurrentThreadMarked(): Boolean = currentThreadMarker.get() == true

  override fun close() {
    closeableDispatcher.close()
  }
}
