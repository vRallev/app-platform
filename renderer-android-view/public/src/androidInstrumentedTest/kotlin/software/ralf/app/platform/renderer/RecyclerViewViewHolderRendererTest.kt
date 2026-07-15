package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.descendants
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel

class RecyclerViewViewHolderRendererTest {
  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @Test
  fun renderModel_is_invoked_for_new_model() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      assertThat(renderer.inflateCalled).isEqualTo(0)
      assertThat(renderer.renderCalled).isEqualTo(0)

      renderer.viewHolder().renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      renderer.viewHolder().renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun the_view_holder_is_cached() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)
      assertThat(renderer.viewHolder()).isSameInstanceAs(renderer.viewHolder())
    }
  }

  @Test
  fun inflate_is_not_invoked_after_detach() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      renderer.viewHolder().renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      activity.contentView.removeAllViews()

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun view_holders_and_nested_renderers_are_reused_while_scrolling() {
    // This test creates a RecyclerView with 200 rows. Each row has a
    // RecyclerViewViewHolderRenderer and each one of them has a nested ViewRenderer.
    //
    // This test verifies that renderers are reused / recycled in the RecyclerView. It also
    // ensures that the nested ViewRenderer is optimized for the recycling behavior and
    // onDetach() does not release the view.

    activityRule.scenario.onActivity { activity -> activity.addRecyclerView() }

    activityRule.scenario.onActivity { activity ->
      assertThat(activity.recyclerView.childTextView("Test: 2")).isNotNull()

      // There must be an equal number of created and bound ViewHolders.
      val adapter = activity.recyclerView.adapter as TestAdapter
      assertThat(adapter.bindCalled).isEqualTo(adapter.createCalled)

      // The renderers were only inflated once (and otherwise recycled).
      assertThat(activity.recyclerView.viewHolderRenderer(0).inflateCalled).isEqualTo(1)
      assertThat(activity.recyclerView.viewHolderRenderer(0).nestedRenderer.inflateCalled)
        .isEqualTo(1)
    }

    smoothScrollTo(200)

    activityRule.scenario.onActivity { activity ->
      assertThat(activity.recyclerView.childTextView("Test: 200")).isNotNull()

      val adapter = activity.recyclerView.adapter as TestAdapter

      // Conservative numbers that account for slow scrolling, stuttering and lag.
      assertThat(adapter.createCalled).isLessThan(140)
      assertThat(adapter.bindCalled).isGreaterThan(150)

      // The renderers were only inflated once (and otherwise recycled).
      for (i in 190..199) {
        assertThat(activity.recyclerView.viewHolderRenderer(i).inflateCalled).isEqualTo(1)
        assertThat(activity.recyclerView.viewHolderRenderer(i).nestedRenderer.inflateCalled)
          .isEqualTo(1)
      }
    }

    smoothScrollTo(0)

    activityRule.scenario.onActivity { activity ->
      assertThat(activity.recyclerView.childTextView("Test: 1")).isNotNull()

      val adapter = activity.recyclerView.adapter as TestAdapter

      // Conservative numbers that account for slow scrolling, stuttering and lag.
      assertThat(adapter.createCalled).isLessThan(180)
      assertThat(adapter.bindCalled).isGreaterThan(300)

      // The renderers were only inflated once (and otherwise recycled).
      for (i in 0..10) {
        assertThat(activity.recyclerView.viewHolderRenderer(i).inflateCalled).isEqualTo(1)
        assertThat(activity.recyclerView.viewHolderRenderer(i).nestedRenderer.inflateCalled)
          .isEqualTo(1)
      }
    }
  }

  private fun renderer(activity: Activity): TestRecyclerViewViewHolderRenderer {
    return TestRecyclerViewViewHolderRenderer().also { it.init(activity, activity.contentView) }
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private val Activity.recyclerView: RecyclerView
    get() = contentView.children.filterIsInstance<RecyclerView>().single()

  private fun Activity.addRecyclerView() {
    val recyclerView = RecyclerView(this)
    val models = List(200) { TestModel(it + 1) }

    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.adapter = TestAdapter(models)

    contentView.addView(recyclerView)
  }

  private fun RecyclerView.childTextView(text: String): TextView? {
    return descendants.filterIsInstance<TextView>().singleOrNull { it.text == text }
  }

  private fun smoothScrollTo(position: Int) {
    activityRule.scenario.onActivity { activity ->
      activity.recyclerView.layoutManager!!.startSmoothScroll(
        object : LinearSmoothScroller(activity) {
          init {
            targetPosition = position
          }

          override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        }
      )
    }

    // Let the scroll from above finish.
    Thread.sleep(2_000L)
  }

  @Suppress("UNCHECKED_CAST")
  private fun RecyclerView.viewHolderRenderer(
    adapterPosition: Int
  ): TestRecyclerViewViewHolderRenderer {
    val viewHolder =
      findViewHolderForAdapterPosition(adapterPosition)
        as RecyclerViewViewHolderRenderer.ViewHolder<TestModel>

    return viewHolder.renderer as TestRecyclerViewViewHolderRenderer
  }

  private data class TestModel(val value: Int) : BaseModel

  private class TestRecyclerViewViewHolderRenderer : RecyclerViewViewHolderRenderer<TestModel>() {

    lateinit var nestedRenderer: NestedViewRenderer
      private set

    var inflateCalled = 0
      private set

    var renderCalled = 0
      private set

    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
    ): View {
      inflateCalled++

      val frameLayout = FrameLayout(activity)

      nestedRenderer = NestedViewRenderer()
      nestedRenderer.init(activity, frameLayout)
      return frameLayout
    }

    override fun render(model: TestModel) {
      renderCalled++
      nestedRenderer.render(model)
    }
  }

  private class NestedViewRenderer : ViewRenderer<TestModel>() {
    private lateinit var textView: TextView

    var inflateCalled = 0
      private set

    var renderCalled = 0
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
  }

  private class TestAdapter(private val models: List<TestModel>) :
    RecyclerView.Adapter<RecyclerViewViewHolderRenderer.ViewHolder<TestModel>>() {

    var createCalled = 0
      private set

    var bindCalled = 0
      private set

    override fun getItemCount(): Int = models.size

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): RecyclerViewViewHolderRenderer.ViewHolder<TestModel> {
      createCalled++

      val renderer = TestRecyclerViewViewHolderRenderer()
      renderer.init(parent.context as Activity, parent)
      return renderer.viewHolder()
    }

    override fun onBindViewHolder(
      holder: RecyclerViewViewHolderRenderer.ViewHolder<TestModel>,
      position: Int,
    ) {
      bindCalled++

      holder.renderer.render(models[position])
    }
  }
}
