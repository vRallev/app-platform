package software.ralf.app.platform.sample.navigation

import androidx.compose.runtime.Composable
import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.test
import software.ralf.app.platform.sample.login.LoginPresenter
import software.ralf.app.platform.sample.user.FakeUserManager
import software.ralf.app.platform.sample.user.UserPagePresenter

class NavigationPresenterImplTest {

  @Test
  fun `after login the presenter navigates from the login screen to the user page screen`() =
    runTest {
      val userManager = FakeUserManager()

      val presenter =
        NavigationPresenterImpl(
          userManager = userManager,
          loginPresenter = { FakeLoginPresenter() },
        )

      presenter.test(this) {
        assertThat(awaitItem()).isInstanceOf<LoginPresenter.Model>()

        userManager.login(
          userId = 1L,
          scope = this@runTest,
          graph =
            object : NavigationPresenterImpl.UserGraph {
              override val userPresenter: UserPagePresenter
                get() = FakeUserPagePresenter()
            },
        )

        assertThat(awaitItem()).isInstanceOf<UserPagePresenter.Model>()
      }
    }

  private class FakeLoginPresenter : LoginPresenter {
    @Composable
    override fun present(input: Unit): LoginPresenter.Model =
      LoginPresenter.Model(loginInProgress = false) {}
  }

  private class FakeUserPagePresenter : UserPagePresenter {
    @Composable
    override fun present(input: Unit): UserPagePresenter.Model =
      object : UserPagePresenter.Model {
        override val listModel: BaseModel = object : BaseModel {}
        override val detailModel: BaseModel = object : BaseModel {}
      }
  }
}
