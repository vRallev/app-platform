@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.presenter.backstack.nav3

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import software.ralf.app.platform.ExperimentalAppPlatform

class PresenterBackstackEntryStoreTest {
  @Test
  fun `sync keeps active keys stable when the stack grows`() {
    val store = PresenterBackstackRenderer.PresenterBackstackEntryStore<String>()

    val rootKeys = store.sync(listOf("welcome"))
    val grownKeys = store.sync(listOf("welcome updated", "sign in"))

    assertThat(rootKeys).isEqualTo(listOf(0))
    assertThat(grownKeys).isEqualTo(listOf(0, 1))
    assertThat(store.entryFor(0)).isEqualTo("welcome updated")
    assertThat(store.entryFor(1)).isEqualTo("sign in")
  }

  @Test
  fun `sync retains a popped entry until it is released`() {
    val store = PresenterBackstackRenderer.PresenterBackstackEntryStore<String>()

    store.sync(listOf("welcome", "sign in"))
    store.retain(1)
    val keysAfterPop = store.sync(listOf("welcome"))

    assertThat(keysAfterPop).isEqualTo(listOf(0))
    assertThat(store.entryFor(1)).isEqualTo("sign in")

    store.release(1)

    assertFailure { store.entryFor(1) }.isInstanceOf<IllegalStateException>()
  }

  @Test
  fun `release keeps active entry retained`() {
    val store = PresenterBackstackRenderer.PresenterBackstackEntryStore<String>()

    store.sync(listOf("welcome", "sign in"))
    store.retain(0)

    store.release(0)

    assertThat(store.entryFor(0)).isEqualTo("welcome")
  }

  @Test
  fun `sync removes a popped entry that is already released`() {
    val store = PresenterBackstackRenderer.PresenterBackstackEntryStore<String>()

    store.sync(listOf("welcome", "sign in"))
    store.retain(1)
    store.release(1)

    store.sync(listOf("welcome"))

    assertFailure { store.entryFor(1) }.isInstanceOf<IllegalStateException>()
  }

  @Test
  fun `sync assigns a fresh key when pushing after a pop`() {
    val store = PresenterBackstackRenderer.PresenterBackstackEntryStore<String>()

    store.sync(listOf("welcome", "sign in"))
    store.sync(listOf("welcome"))
    val keysAfterRepush = store.sync(listOf("welcome", "forgot password"))

    assertThat(keysAfterRepush).isEqualTo(listOf(0, 2))
    assertThat(store.entryFor(2)).isEqualTo("forgot password")
  }
}
