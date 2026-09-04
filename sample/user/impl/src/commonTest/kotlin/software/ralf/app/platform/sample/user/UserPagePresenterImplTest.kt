package software.ralf.app.platform.sample.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.compose.backgesture.withBackGestureDispatcher
import software.ralf.app.platform.presenter.compose.test
import software.ralf.app.platform.scope.runTestWithScope

class UserPagePresenterImplTest {

  @Test
  fun `the selected item can be changed`() = runTest {
    val userManager = FakeUserManager()
    userManager.login(1L)
    val user = checkNotNull(userManager.user.value)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)

    val presenter =
      UserPagePresenterImpl(
        user,
        FakeUserManager(),
        UserPageListPresenter(sessionTimeout),
        UserPageDetailPresenter(sessionTimeout),
      )

    presenter.withBackGestureDispatcher().test(this) {
      awaitItem().let { model ->
        assertThat((model.listModel as UserPageListPresenter.Model).selectedIndex).isEqualTo(0)
        assertThat((model.detailModel as UserPageDetailPresenter.Model).text)
          .isEqualTo(FakeUser.fakeAttribute1.value)

        (model.listModel as UserPageListPresenter.Model).onEvent(
          UserPageListPresenter.Event.ItemSelected(1)
        )
      }

      awaitItem().let { model ->
        assertThat((model.listModel as UserPageListPresenter.Model).selectedIndex).isEqualTo(1)
        assertThat((model.detailModel as UserPageDetailPresenter.Model).text)
          .isEqualTo(FakeUser.fakeAttribute2.value)
      }
    }
  }

  @Test
  fun `the session timeout progress is updated`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)
    val user = checkNotNull(userManager.user.value)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    scope.register(sessionTimeout)

    val presenter =
      UserPagePresenterImpl(
        user,
        userManager,
        UserPageListPresenter(sessionTimeout),
        UserPageDetailPresenter(sessionTimeout),
      )

    presenter.withBackGestureDispatcher().test(this) {
      val model = awaitItem().detailModel as UserPageDetailPresenter.Model
      assertThat(model.timeoutProgress.value).isEqualTo(1f)

      advanceTimeBy(SessionTimeout.initialTimeout / 2)
      runCurrent()

      // This is important. This test only modifies the value for timeoutProgress.
      // This property is a composable `State` and therefore changes aren't propagated
      // through Model updates, but instead the property must be observed.
      expectNoEvents()

      assertThat(model.timeoutProgress.value).isEqualTo(0.5f)
    }
  }

  @Test
  fun `the user logs out on back press`() =
    runTestWithScope(UnconfinedTestDispatcher()) { scope ->
      val userManager = FakeUserManager()
      userManager.login(1L)
      val user = checkNotNull(userManager.user.value)

      val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
      scope.register(sessionTimeout)

      val presenter =
        UserPagePresenterImpl(
          user,
          userManager,
          UserPageListPresenter(sessionTimeout),
          UserPageDetailPresenter(sessionTimeout),
        )

      val backEvent = MutableSharedFlow<Unit>()
      presenter.withBackGestureDispatcher(backEvent).test(this) {
        assertThat(awaitItem()).isNotNull()

        backEvent.emit(Unit)

        expectNoEvents()
        assertThat(userManager.user.value).isNull()
      }
    }
}
