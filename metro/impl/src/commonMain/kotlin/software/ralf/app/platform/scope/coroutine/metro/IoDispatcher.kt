package software.ralf.app.platform.scope.coroutine.metro

import kotlinx.coroutines.CoroutineDispatcher

/** Expect declaration for the IO dispatcher, because it doesn't exist for WASM. */
internal expect val ioDispatcher: CoroutineDispatcher
