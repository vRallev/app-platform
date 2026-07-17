package software.ralf.app.platform.template.navigation

import androidx.compose.runtime.Composable
import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.molecule.test
import software.ralf.app.platform.template.templates.AppTemplate

class NavigationPresenterImplTest {

  @Test
  fun `correct template and presenter models are returned`() = runTest {
    val presenter =
      NavigationPresenterImpl(
        navigationHeaderPresenter = FakeNavigationHeaderPresenter(),
        navigationDetailPresenter = FakeNavigationDetailPresenter(),
      )

    presenter.test(this) {
      awaitItem().let { template ->
        assertThat(template).isInstanceOf<AppTemplate.HeaderDetailTemplate>()
        (template as? AppTemplate.HeaderDetailTemplate)?.let { headerDetailTemplate ->
          assertThat(headerDetailTemplate.header).isInstanceOf<NavigationHeaderPresenter.Model>()
          assertThat(headerDetailTemplate.detail).isInstanceOf<NavigationDetailPresenter.Model>()
        }
      }
    }
  }

  private class FakeNavigationDetailPresenter : NavigationDetailPresenter {
    @Composable
    override fun present(input: Unit): NavigationDetailPresenter.Model =
      NavigationDetailPresenter.Model(exampleValue = 5, exampleCount = 1)
  }

  private class FakeNavigationHeaderPresenter : NavigationHeaderPresenter {
    @Composable
    override fun present(input: Unit): NavigationHeaderPresenter.Model =
      NavigationHeaderPresenter.Model(clickedCount = 0) {}
  }
}
