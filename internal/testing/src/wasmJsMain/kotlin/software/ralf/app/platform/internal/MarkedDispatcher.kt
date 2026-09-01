package software.ralf.app.platform.internal

/** This JVM-only test helper is unavailable on Wasm targets. */
actual fun createMarkedDispatcher(): MarkedDispatcher = throw NotImplementedError()
