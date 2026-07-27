package software.ralf.app.platform.listdetail.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ScreenSizeTest {
  @Test
  fun `wide landscape window is a tablet`() {
    assertCategory(width = 600.dp, height = 599.dp, expected = ScreenSize.Category.TABLET)
  }

  @Test
  fun `square window is a phone`() {
    assertCategory(width = 600.dp, height = 600.dp, expected = ScreenSize.Category.PHONE)
  }

  @Test
  fun `portrait window is a phone regardless of width`() {
    assertCategory(width = 900.dp, height = 1200.dp, expected = ScreenSize.Category.PHONE)
  }

  private fun assertCategory(width: Dp, height: Dp, expected: ScreenSize.Category) {
    assertThat(ScreenSize.from(width = width, height = height).category).isEqualTo(expected)
  }
}
