package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import assertk.assertions.messageContains
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel

class AndroidRendererFactoryTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @Before
  fun prepare() {
    testApplication.rendererComponent = TestRendererComponent()
  }

  @After
  fun tearDown() {
    testApplication.rendererComponent = null
  }

  @Test
  fun android_renderer_factory_uses_the_default_parent_view() {
    activityRule.scenario.onActivity { activity ->
      val factory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val model = TestModel(1)
      factory.getRenderer(model).render(model)

      assertThat(activity.testView.text.toString()).isEqualTo("Test: 1")
    }
  }

  @Test
  fun android_renderer_factory_can_override_the_parent_view() {
    activityRule.scenario.onActivity { activity ->
      val factory: RendererFactory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val container = FrameLayout(activity)
      activity.contentView.addView(container)

      val model = TestModel(1)
      factory.getRenderer(model::class, container).render(model)

      assertThat(activity.testView.text.toString()).isEqualTo("Test: 1")
    }
  }

  @Test
  fun android_renderer_factory_caches_renderers() {
    activityRule.scenario.onActivity { activity ->
      val factory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val actualRenderer = factory.getRenderer(TestModel(1))
      val expectedRenderer = factory.getRenderer(TestModel(2))

      assertThat(actualRenderer).isSameInstanceAs(expectedRenderer)
    }
  }

  @Test
  fun android_renderer_factory_caches_renderers_based_on_parent_view() {
    activityRule.scenario.onActivity { activity ->
      val frameLayout1 = FrameLayout(activity)
      val frameLayout2 = FrameLayout(activity)

      val linearLayout =
        LinearLayout(activity).apply {
          addView(frameLayout1)
          addView(frameLayout2)
        }

      activity.contentView.addView(linearLayout)

      val factory: BaseRendererFactory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val renderer1 = factory.getRenderer(TestModel::class, frameLayout1)
      renderer1.render(TestModel(1))

      val renderer2 = factory.getRenderer(TestModel::class, frameLayout2)
      renderer2.render(TestModel(2))

      assertThat(renderer1).isNotSameInstanceAs(renderer2)
      assertThat(renderer1).isSameInstanceAs(factory.getRenderer(TestModel::class, frameLayout1))
      assertThat(renderer2).isSameInstanceAs(factory.getRenderer(TestModel::class, frameLayout2))

      assertThat(frameLayout1.testView.text.toString()).isEqualTo("Test: 1")
      assertThat(frameLayout2.testView.text.toString()).isEqualTo("Test: 2")
    }
  }

  @Test
  fun android_renderer_factory_uses_a_different_cache_key_for_different_renderer_ids() {
    activityRule.scenario.onActivity { activity ->
      val linearLayout = LinearLayout(activity)

      val factory: BaseRendererFactory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val renderer1 = factory.getRenderer(TestModel::class, linearLayout, rendererId = 1)
      val renderer2 = factory.getRenderer(TestModel::class, linearLayout, rendererId = 1)
      val renderer3 = factory.getRenderer(TestModel::class, linearLayout, rendererId = 2)

      assertThat(renderer1).isSameInstanceAs(renderer2)
      assertThat(renderer1).isNotSameInstanceAs(renderer3)
    }
  }

  @Test
  fun android_renderer_factory_allows_to_change_the_parent_view() {
    activityRule.scenario.onActivity { activity ->
      val frameLayout = FrameLayout(activity)
      activity.contentView.addView(frameLayout)

      val factory: BaseRendererFactory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val renderer = factory.getRenderer(TestModel::class, frameLayout)
      renderer.render(TestModel(2))

      assertThat(activity.testView.text.toString()).isEqualTo("Test: 2")

      assertThat(activity.contentView.childCount).isEqualTo(1)
      assertThat(activity.contentView.children.single()).isSameInstanceAs(frameLayout)
    }
  }

  @Test
  fun an_unknown_model_type_gives_a_meaningful_error() {
    testApplication.rendererComponent =
      object : RendererComponent {
        override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> = emptyMap()
        override val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
          emptyMap()
      }

    activityRule.scenario.onActivity { activity ->
      val factory =
        AndroidRendererFactory(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = activity.contentView,
        )

      val exception = assertFailsWith<IllegalStateException> { factory.getRenderer(TestModel(1)) }

      assertThat(exception)
        .messageContains(
          "No renderer was provided for class " +
            "software.ralf.app.platform.renderer.AndroidRendererFactoryTest" +
            "\$TestModel (Kotlin reflection is not available). " +
            "Did you add @ContributesRenderer?"
        )
    }
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private val Activity.testView: TextView
    get() = contentView.testView

  private val View.testView: TextView
    get() = findViewWithTag("testView")

  private data class TestModel(val value: Int) : BaseModel

  private class TestViewRenderer : ViewRenderer<TestModel>() {

    private lateinit var textView: TextView

    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: TestModel,
    ): View =
      TextView(activity).also {
        textView = it
        textView.tag = "testView"
      }

    override fun renderModel(model: TestModel) {
      textView.text = "Test: ${model.value}"
    }
  }

  private class TestRendererComponent : RendererComponent {
    override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
      mapOf(TestModel::class to { TestViewRenderer() })
    override val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
      mapOf(TestModel::class to TestViewRenderer::class)
  }
}
