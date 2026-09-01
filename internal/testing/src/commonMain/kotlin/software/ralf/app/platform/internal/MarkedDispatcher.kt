package software.ralf.app.platform.internal

import kotlinx.coroutines.CoroutineDispatcher

interface MarkedDispatcher {
  val dispatcher: CoroutineDispatcher

  fun isCurrentThreadMarked(): Boolean

  fun close()
}

expect fun createMarkedDispatcher(): MarkedDispatcher
