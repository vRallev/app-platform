package software.ralf.app.platform.internal

/** Provides the name of the current thread this is called on. */
actual val currentThreadName: String
  get() = Thread.currentThread().name
