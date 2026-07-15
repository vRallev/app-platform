package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.android.view.test.databinding.ViewbindingLayoutBinding

class ViewBindingRendererTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @Test
  fun renderModel_is_invoked_for_new_model() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      assertThat(renderer.inflateCalled).isEqualTo(0)
      assertThat(renderer.renderCalled).isEqualTo(0)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  private fun renderer(activity: Activity): TestViewBindingRenderer {
    return TestViewBindingRenderer().also { it.init(activity, activity.contentView) }
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private data class TestModel(val value: Int) : BaseModel

  private class TestViewBindingRenderer :
    ViewBindingRenderer<TestModel, ViewbindingLayoutBinding>() {

    var inflateCalled = 0
      private set

    var renderCalled = 0
      private set

    override fun inflateViewBinding(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: TestModel,
    ): ViewbindingLayoutBinding {
      inflateCalled++
      return ViewbindingLayoutBinding.inflate(layoutInflater, parent, false)
    }

    override fun renderModel(model: TestModel) {
      binding.viewbindingTextView.text = "Test: ${model.value}"
      renderCalled++
    }
  }
}
