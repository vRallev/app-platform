package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel

class ViewRendererTest {

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

  @Test
  fun renderModel_is_not_invoked_for_equal_model() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      assertThat(renderer.inflateCalled).isEqualTo(0)
      assertThat(renderer.renderCalled).isEqualTo(0)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)
    }
  }

  @Test
  fun inflate_is_invoked_after_detach() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      activity.contentView.removeAllViews()

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(2)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun inflate_is_not_invoked_after_detach_when_not_released() {
    activityRule.scenario.onActivity { activity ->
      val renderer =
        TestViewRenderer(releaseViewOnDetach = false).also {
          it.init(activity, activity.contentView)
        }

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      activity.contentView.removeAllViews()

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun renderModel_provides_the_previous_model() {
    activityRule.scenario.onActivity { activity ->
      var currentModel: TestModel? = null
      var previousModel: TestModel? = null

      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View = View(activity)

          override fun renderModel(model: TestModel, lastModel: TestModel?) {
            currentModel = model
            previousModel = lastModel
          }
        }

      renderer.init(activity, activity.contentView)

      renderer.render(TestModel(1))
      assertThat(currentModel).isEqualTo(TestModel(1))
      assertThat(previousModel).isNull()

      renderer.render(TestModel(2))
      assertThat(currentModel).isEqualTo(TestModel(2))
      assertThat(previousModel).isEqualTo(TestModel(1))

      activity.contentView.removeAllViews()

      renderer.render(TestModel(3))
      assertThat(currentModel).isEqualTo(TestModel(3))
      assertThat(previousModel).isNull()
    }
  }

  @Test
  fun the_coroutine_scope_is_canceled_after_detach() {
    activityRule.scenario.onActivity { activity ->
      lateinit var coroutineScope: CoroutineScope

      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View {
            coroutineScope = this.coroutineScope
            return View(activity)
          }
        }

      renderer.init(activity, activity.contentView)

      renderer.render(TestModel(1))
      assertThat(coroutineScope.isActive).isTrue()

      val oldScope = coroutineScope

      activity.contentView.removeAllViews()
      assertThat(coroutineScope.isActive).isFalse()

      renderer.render(TestModel(2))
      assertThat(coroutineScope.isActive).isTrue()
      assertThat(coroutineScope).isNotSameInstanceAs(oldScope)
    }
  }

  @Test
  fun the_coroutine_scope_is_canceled_when_the_view_was_never_attached() {
    lateinit var coroutineScope: CoroutineScope

    activityRule.scenario.onActivity { activity ->
      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View {
            coroutineScope = this.coroutineScope
            return View(activity)
          }
        }

      // Note that this parent is never attached to the view hierarchy and therefore onDetach
      // never gets called.
      val parent = FrameLayout(activity)
      renderer.init(activity, parent)

      renderer.render(TestModel(1))
      assertThat(coroutineScope.isActive).isTrue()

      activity.finish()
    }

    activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)

    assertThat(coroutineScope.isActive).isFalse()
  }

  @Test
  fun the_coroutine_scope_is_canceled_when_the_view_is_not_released_on_detach() {
    lateinit var coroutineScope: CoroutineScope

    activityRule.scenario.onActivity { activity ->
      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View {
            coroutineScope = this.coroutineScope
            return View(activity)
          }

          override fun releaseViewOnDetach(): Boolean = false
        }

      renderer.init(activity, activity.contentView)

      renderer.render(TestModel(1))
      assertThat(coroutineScope.isActive).isTrue()
    }

    activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    assertThat(coroutineScope.isActive).isTrue()

    activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
    assertThat(coroutineScope.isActive).isFalse()
  }

  @Test
  fun onDetach_is_called_once() {
    // This test verifies an edge case we suppressed for a long time. The crash happened in the
    // ViewRenderer implementation when not correctly unregistering onAttach / onDetach callbacks.
    //
    // java.lang.NullPointerException: Attempt to write to field 'android.view.ViewParent
    // android.view.View.mParent' on a null object reference
    //   at android.view.ViewGroup.removeFromArray(ViewGroup.java:5384)
    //   at android.view.ViewGroup.removeViewInternal(ViewGroup.java:5581)
    //   at android.view.ViewGroup.removeViewInternal(ViewGroup.java:5543)
    //   at android.view.ViewGroup.removeView(ViewGroup.java:5474)

    activityRule.scenario.onActivity { activity ->
      val grandParent = FrameLayout(activity)
      activity.setContentView(grandParent)

      val parent = FrameLayout(activity)

      // To trigger the crash it is important that the 'parent' container is not attached to the
      // view hierarchy yet (not a child of 'grandParent' yet).
      val renderer = renderer(activity, parent)
      renderer.render(TestModel(1))
      // By removing the view we'll add it to the 'parent' in the next render() call again. The bug
      // used to be that we didn't clear the callbacks. In production the issue also manifested
      // during Activity.onCreate() before Activity.onStart() without explicitly removing all views.
      parent.removeAllViews()
      renderer.render(TestModel(2))

      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
      assertThat(renderer.onDetachCalled).isEqualTo(0)

      // Adding the view invoked the onAttach callback and removing it will invoke the onDetach
      // callback. In the past without clearing the callbacks properly onDetach was called
      // twice and triggered the exception.
      grandParent.addView(parent)
      grandParent.removeAllViews()

      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
      assertThat(renderer.onDetachCalled).isEqualTo(1)
    }
  }

  @Test
  fun it_is_forbidden_to_change_the_parent() {
    activityRule.scenario.onActivity { activity ->
      val parent1 = FrameLayout(activity)
      val parent2 = FrameLayout(activity)

      val renderer = TestViewRenderer()

      renderer.init(activity, parent1)
      // It is allowed to change the parent before the view gets created.
      renderer.init(activity, parent2)

      // This creates the view.
      renderer.render(TestModel(1))

      assertFailure { renderer.init(activity, parent1) }
        .messageContains("A ViewRenderer should ever be only attached to one parent view.")
    }
  }

  @Test
  fun multiple_renderers_with_the_same_parent_can_be_detached_when_the_activity_is_destroyed() {
    val renderers = List(10) { TestViewRenderer() }

    activityRule.scenario.onActivity { activity ->
      val parent = FrameLayout(activity).also { activity.contentView.addView(it) }

      renderers.forEachIndexed { index, renderer ->
        renderer.init(activity, parent)

        // This creates and attaches the view.
        renderer.render(TestModel(index))
      }
    }

    activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

    activityRule.scenario.onActivity { activity ->
      val childViews =
        activity.contentView.children.filterIsInstance<ViewGroup>().single().children.toList()

      childViews.forEach { assertThat(it.isAttachedToWindow).isTrue() }
      renderers.forEach {
        assertThat(it.inflateCalled).isEqualTo(1)
        assertThat(it.renderCalled).isEqualTo(1)
        assertThat(it.onDetachCalled).isEqualTo(0)
      }
    }

    activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)

    // Wait for the idle sync, otherwise not all onDetach callbacks may have been invoked.
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    renderers.forEach {
      assertThat(it.inflateCalled).isEqualTo(1)
      assertThat(it.renderCalled).isEqualTo(1)
      assertThat(it.onDetachCalled).isEqualTo(1)
    }
  }

  private fun renderer(
    activity: Activity,
    parent: ViewGroup = activity.contentView,
  ): TestViewRenderer {
    return TestViewRenderer().also { it.init(activity, parent) }
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private data class TestModel(val value: Int) : BaseModel

  private class TestViewRenderer(private val releaseViewOnDetach: Boolean = true) :
    ViewRenderer<TestModel>() {

    private lateinit var textView: TextView

    var inflateCalled = 0
      private set

    var renderCalled = 0
      private set

    var onDetachCalled = 0
      private set

    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: TestModel,
    ): View =
      TextView(activity).also {
        textView = it
        inflateCalled++
      }

    override fun renderModel(model: TestModel) {
      textView.text = "Test: ${model.value}"
      renderCalled++
    }

    override fun releaseViewOnDetach(): Boolean {
      return releaseViewOnDetach
    }

    override fun onDetach() {
      onDetachCalled++
    }
  }
}
