@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.navigation

import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.FakePresenterBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.withPresenterBackstackScope
import software.ralf.app.platform.presenter.compose.test
import software.ralf.app.platform.recipes.landing.LandingPresenter

class LoginPresenterTest {
  @Test
  fun `email presenter reports completion through its model`() = runTest {
    EmailPresenter().test(this) {
      val content = assertIs<EmailPresenter.Model.Content>(awaitItem())

      content.onNext()

      val done = assertIs<EmailPresenter.Model.Done>(awaitItem())
      assertThat(done.content.email).isSameInstanceAs(content.email)
    }
  }

  @Test
  fun `password presenter reports both completion outcomes through its model`() = runTest {
    PasswordPresenter().test(this) {
      val content = assertIs<PasswordPresenter.Model.Content>(awaitItem())
      content.onBack()
      assertIs<PasswordPresenter.Model.Back>(awaitItem())
    }

    PasswordPresenter().test(this) {
      val content = assertIs<PasswordPresenter.Model.Content>(awaitItem())
      content.onDone()
      assertIs<PasswordPresenter.Model.Done>(awaitItem())
    }
  }

  @Test
  fun `parent interprets child completion models`() = runTest {
    val presenter = LoginPresenter()

    presenter.withPresenterBackstackScope().test(this) {
      awaitModel<EmailPresenter.Model.Content>().onNext()

      awaitModel<PasswordPresenter.Model.Content>().onBack()

      awaitModel<EmailPresenter.Model.Content>()
    }
  }

  @Test
  fun `done returns to the recipe landing screen`() = runTest {
    val testScope = this
    val landingPresenter = LandingPresenter()
    val backstack = FakePresenterBackstackScope(landingPresenter)

    landingPresenter.withPresenterBackstackScope(backstack).test(this) {
      awaitItem().onEvent(LandingPresenter.Event.PresenterNavigation)
    }

    val loginPresenter =
      assertIs<LoginPresenter>(backstack.lastBackstackChange.value.backstack.last())

    loginPresenter.withPresenterBackstackScope(backstack).test(this) {
      awaitModel<EmailPresenter.Model.Content>().onNext()
      awaitModel<PasswordPresenter.Model.Content>().onDone()
      awaitModel<PasswordPresenter.Model.Done>()
      testScope.runCurrent()
    }

    assertThat(backstack.lastBackstackChange.value.backstack).containsExactly(landingPresenter)
  }

  private suspend inline fun <reified ModelT : BaseModel> ReceiveTurbine<BaseModel>.awaitModel():
    ModelT {
    while (true) {
      val model = awaitItem()
      if (model is ModelT) {
        return model
      }
    }
  }
}
