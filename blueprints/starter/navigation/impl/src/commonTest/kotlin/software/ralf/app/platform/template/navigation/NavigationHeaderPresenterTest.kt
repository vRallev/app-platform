package software.ralf.app.platform.template.navigation

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.molecule.test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationHeaderPresenterTest {

  @Test
  fun `correctly process and emit model when Clicked event is triggered`() = runTest {
    NavigationHeaderPresenterImpl().test(this) {
      awaitItem().let { model ->
        assertThat(model.clickedCount).isEqualTo(0)
        model.onEvent(NavigationHeaderPresenter.Event.Clicked)
      }

      awaitItem().let { model ->
        assertThat(model.clickedCount).isEqualTo(1)
        model.onEvent(NavigationHeaderPresenter.Event.Clicked)
      }

      awaitItem().let { model -> assertThat(model.clickedCount).isEqualTo(2) }
    }
  }
}
