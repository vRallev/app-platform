package software.ralf.app.platform.presenter.compose.backgesture

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.renderer.TestActivity

class ForwardBackPressEventsToPresentersAndroidTest {
  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @Test
  fun back_press_events_are_forwarded_to_presenters() {
    val dispatcher = InterceptorBackGestureDispatcherPresenter()

    activityRule.scenario.onActivity { activity ->
      dispatcher.forwardBackPressEventsToPresenters(activity)
    }

    assertThat(dispatcher.onPredictiveBackCount).isEqualTo(0)

    Espresso.pressBack()
    assertThat(dispatcher.onPredictiveBackCount).isEqualTo(1)

    Espresso.pressBack()
    assertThat(dispatcher.onPredictiveBackCount).isEqualTo(2)
  }

  private class InterceptorBackGestureDispatcherPresenter :
    BackGestureDispatcherPresenter by BackGestureDispatcherPresenter.createNewInstance() {
    override val listenersCount: StateFlow<Int> = MutableStateFlow(1)

    var onPredictiveBackCount = 0

    override suspend fun onPredictiveBack(progress: Flow<BackEventPresenter>) {
      progress.collect {}
      onPredictiveBackCount++
    }
  }
}
