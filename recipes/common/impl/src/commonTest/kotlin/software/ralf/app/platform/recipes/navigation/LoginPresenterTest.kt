@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.backstack.nav3.FakePresenterBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.withPresenterBackstackScope
import software.ralf.app.platform.presenter.compose.test
import software.ralf.app.platform.recipes.landing.LandingPresenter

class LoginPresenterTest {
  @Test
  fun `parent handles navigation between child presenters`() = runTest {
    val presenter = LoginPresenter()

    presenter.withPresenterBackstackScope().test(this) {
      assertIs<LoginPresenter.Model.Email>(awaitItem()).onNext()

      assertIs<LoginPresenter.Model.Password>(awaitItem()).onBack()

      assertIs<LoginPresenter.Model.Email>(awaitItem())
    }
  }

  @Test
  fun `done returns to the recipe landing screen`() = runTest {
    val landingPresenter = LandingPresenter()
    val backstack = FakePresenterBackstackScope(landingPresenter)

    landingPresenter.withPresenterBackstackScope(backstack).test(this) {
      awaitItem().onEvent(LandingPresenter.Event.PresenterNavigation)
    }

    val loginPresenter =
      assertIs<LoginPresenter>(backstack.lastBackstackChange.value.backstack.last())

    loginPresenter.withPresenterBackstackScope(backstack).test(this) {
      assertIs<LoginPresenter.Model.Email>(awaitItem()).onNext()
      assertIs<LoginPresenter.Model.Password>(awaitItem()).onDone()
    }

    assertThat(backstack.lastBackstackChange.value.backstack).containsExactly(landingPresenter)
  }
}
