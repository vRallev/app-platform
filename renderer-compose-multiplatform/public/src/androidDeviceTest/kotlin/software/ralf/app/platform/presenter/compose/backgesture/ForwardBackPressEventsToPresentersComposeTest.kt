package software.ralf.app.platform.presenter.compose.backgesture

import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.compose.ComposePresenter
import software.ralf.app.platform.presenter.compose.backgesture.ForwardBackPressEventsToPresentersComposeTest.TestPresenter.Model
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.TestActivity
import software.ralf.app.platform.renderer.getActivityFromTestRule

class ForwardBackPressEventsToPresentersComposeTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @get:Rule
  val composeTestRule =
    AndroidComposeTestRule(activityRule, activityProvider = { getActivityFromTestRule(it) })

  @Test
  fun back_press_events_are_forwarded_to_presenters() {
    val backGestureDispatcherPresenter = BackGestureDispatcherPresenter.createNewInstance()

    val testPresenter = TestPresenter(backGestureDispatcherPresenter)
    val testRenderer = TestRenderer()
    val rootRenderer = RootRenderer(backGestureDispatcherPresenter, testRenderer)

    activityRule.scenario.onActivity { activity ->
      activity.setContent {
        val model = testPresenter.present(Unit)
        rootRenderer.renderCompose(model)
      }
    }

    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 0")

    Espresso.pressBack()
    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 1")

    Espresso.pressBack()
    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 2")
  }

  private class RootRenderer(
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
    private val testRenderer: TestRenderer,
  ) : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model, modifier: Modifier) {
      backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

      testRenderer.renderCompose(model)
    }
  }

  private class TestPresenter(
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter
  ) : ComposePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      return withCompositionLocal(
        LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
      ) {
        var backPressCount by remember { mutableIntStateOf(0) }

        BackHandlerPresenter { backPressCount++ }

        Model(backPressCount = backPressCount)
      }
    }

    data class Model(val backPressCount: Int) : BaseModel
  }

  private class TestRenderer : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model, modifier: Modifier) {
      BasicText(text = "Count: ${model.backPressCount}", modifier = Modifier.testTag("count"))
    }
  }
}
