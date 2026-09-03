/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.ralf.app.platform.presenter.molecule

import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import software.ralf.app.platform.ExperimentalAppPlatform

/**
 * Installs [store] over [content] and returns the result of the lambda.
 *
 * This has the same guarantees and semantics as `LocalRetainedValuesStoreProvider`, with the
 * addition of a return value.
 */
@ExperimentalAppPlatform
@Composable
public fun <R> withLocalRetainedValuesStore(
  store: RetainedValuesStore,
  content: @Composable () -> R,
): R {
  val result = withCompositionLocal(LocalRetainedValuesStore provides store, content)

  // Important: This must come AFTER the content for the underlying RememberObservers to dispatch
  // in the correct order relative to retained values from the content block.
  val composer = currentComposer
  remember(store) { RetainContentPresenceIndicator(store, composer) }
    .apply {
      // Composer isn't guaranteed to stay the same between recompositions, make sure to update the
      // reference just in case.
      this.composer = composer
    }

  return result
}

private class RetainContentPresenceIndicator(
  private val store: RetainedValuesStore,
  composer: Composer,
) : RememberObserver {
  // Backed by snapshot like rememberUpdatedState to ensure that writes happen at the end of
  // composition without relying on a SideEffect, which will have the wrong timing.
  var composer by mutableStateOf(composer)

  private var didEnterComposition = false
  private var enterCompositionCancellationHandle: CancellationHandle? = null
    set(value) {
      field?.cancel()
      field = value
    }

  override fun onRemembered() {
    enterCompositionCancellationHandle = composer.scheduleFrameEndCallback {
      didEnterComposition = true
      store.onContentEnteredComposition()
    }
  }

  override fun onForgotten() {
    enterCompositionCancellationHandle?.cancel()
    if (didEnterComposition) {
      store.onContentExitComposition()
      didEnterComposition = false
    }
  }

  override fun onAbandoned() {
    enterCompositionCancellationHandle?.cancel()
  }
}
