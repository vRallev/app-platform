package software.ralf.app.platform.sample.login

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.molecule.test
import software.ralf.app.platform.sample.user.FakeUserManager

class LoginPresenterImplTest {

  @Test
  fun `after 1 second the user is logged in after pressing the login button`() = runTest {
    val userManager = FakeUserManager()
    LoginPresenterImpl(userManager).test(this) {
      awaitItem().let { model ->
        assertThat(model.loginInProgress).isFalse()
        model.onEvent(LoginPresenter.Event.Login("userName"))
      }

      assertThat(awaitItem().loginInProgress).isTrue()
      assertThat(userManager.user.value).isNull()

      advanceTimeBy(1.seconds + 1.milliseconds)

      assertThat(awaitItem().loginInProgress).isFalse()
      assertThat(userManager.user.value).isNotNull()
    }
  }
}
