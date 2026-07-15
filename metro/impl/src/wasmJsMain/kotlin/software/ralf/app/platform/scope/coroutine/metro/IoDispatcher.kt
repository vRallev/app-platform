package software.ralf.app.platform.scope.coroutine.metro

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Expect declaration for the IO dispatcher, because it doesn't exist for WASM. */
// Fallback to the Default dispatcher.
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
