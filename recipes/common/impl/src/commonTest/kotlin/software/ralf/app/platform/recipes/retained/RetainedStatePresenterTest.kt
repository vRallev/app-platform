@file:OptIn(ExperimentalAppPlatform::class)

package software.ralf.app.platform.recipes.retained

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.presenter.backstack.nav3.FakePresenterBackstackScope
import software.ralf.app.platform.presenter.backstack.nav3.withPresenterBackstackScope
import software.ralf.app.platform.presenter.compose.test
import software.ralf.app.platform.recipes.landing.LandingPresenter

class RetainedStatePresenterTest {
  @Test
  fun `email and password values are retained while moving between steps`() = runTest {
    val presenter = RetainedStatePresenter()

    presenter.withPresenterBackstackScope().test(this) {
      val emailModel = assertIs<RetainedStatePresenter.Model.Email>(awaitItem())
      emailModel.email.replaceText("email value")
      emailModel.onNext()

      val passwordModel = assertIs<RetainedStatePresenter.Model.Password>(awaitItem())
      passwordModel.password.replaceText("password value")
      passwordModel.onBack()

      val restoredEmailModel = assertIs<RetainedStatePresenter.Model.Email>(awaitItem())
      assertThat(restoredEmailModel.email.value).isEqualTo("email value")
      restoredEmailModel.onNext()

      val restoredPasswordModel = assertIs<RetainedStatePresenter.Model.Password>(awaitItem())
      assertThat(restoredPasswordModel.password.value).isEqualTo("password value")
    }
  }

  @Test
  fun `done returns to the recipe landing screen`() = runTest {
    val landingPresenter = LandingPresenter()
    val backstack = FakePresenterBackstackScope(landingPresenter)

    landingPresenter.withPresenterBackstackScope(backstack).test(this) {
      awaitItem().onEvent(LandingPresenter.Event.RetainedPresenterState)
    }

    val retainedStatePresenter =
      assertIs<RetainedStatePresenter>(backstack.lastBackstackChange.value.backstack.last())

    retainedStatePresenter.withPresenterBackstackScope(backstack).test(this) {
      assertIs<RetainedStatePresenter.Model.Email>(awaitItem()).onNext()
      assertIs<RetainedStatePresenter.Model.Password>(awaitItem()).onDone()
    }

    assertThat(backstack.lastBackstackChange.value.backstack).containsExactly(landingPresenter)
  }
}
