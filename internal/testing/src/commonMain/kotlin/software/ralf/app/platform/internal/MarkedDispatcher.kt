package software.ralf.app.platform.internal

import kotlinx.coroutines.CoroutineDispatcher

/** A closeable dispatcher that reports whether code is running on its managed thread. */
interface MarkedDispatcher {
  /** The dispatcher whose work is marked while it runs. */
  val dispatcher: CoroutineDispatcher

  /** Returns whether the current call is running as work dispatched through [dispatcher]. */
  fun isCurrentThreadMarked(): Boolean

  /** Releases the resources owned by [dispatcher]. */
  fun close()
}

/** Creates a dispatcher for JVM-only tests that distinguish inline from dispatched work. */
expect fun createMarkedDispatcher(): MarkedDispatcher
