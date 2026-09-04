package software.ralf.app.platform.presenter.compose.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import software.ralf.app.platform.ExperimentalAppPlatform

@OptIn(ExperimentalAppPlatform::class)
class PresenterTextFieldStateTest {
  @Test
  fun `exposes the initial text value`() {
    val state = PresenterTextFieldState("initial")

    assertThat(state.value).isEqualTo("initial")
  }

  @Test
  fun `replaces and clears text`() {
    val state = PresenterTextFieldState("initial")

    state.replaceText("updated")
    assertThat(state.value).isEqualTo("updated")

    state.clearText()
    assertThat(state.value).isEqualTo("")
  }
}
