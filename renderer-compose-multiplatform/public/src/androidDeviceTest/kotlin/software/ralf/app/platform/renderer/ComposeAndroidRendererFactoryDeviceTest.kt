package software.ralf.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onSiblings
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.messageContains
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.ralf.app.platform.presenter.BaseModel

@ExperimentalTestApi
class ComposeAndroidRendererFactoryDeviceTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @get:Rule
  val composeTestRule =
    AndroidComposeTestRule(activityRule, activityProvider = { getActivityFromTestRule(it) })

  private lateinit var activity: TestActivity
  private lateinit var factory: RendererFactory

  private var createdRenderers = 0

  @Before
  fun prepare() {
    testApplication.rendererGraph = TestRendererGraph { factory }

    activityRule.scenario.onActivity {
      activity = it

      val container = LinearLayout(activity)
      activity.setContentView(container)

      factory =
        ComposeAndroidRendererFactory.createForAndroidViews(
          rootScopeProvider = testApplication,
          activity = activity,
          parent = container,
        )
    }
  }

  @After
  fun tearDown() {
    // The view containers seem to get confused and crash after the test run finished while
    // tearing down the Activity. This workaround of manually removing the views solves it.
    composeTestRule.runOnUiThread {
      val childAt = activity.contentView.getChildAt(0) as ViewGroup
      childAt.removeAllViews()
      childAt
    }

    testApplication.rendererGraph = null
  }

  @Test
  fun a_compose_renderer_renders_content_on_screen() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val composeModel = ComposeModel(it)
        factory.getRenderer(composeModel).render(composeModel)
      }

      composeTestRule.onNodeWithTag("testCompose").assertTextEquals("Compose test: $it")
    }

    assertThat(createdRenderers).isEqualTo(1)
  }

  @Test
  fun a_compose_renderer_renders_content_on_screen_in_compose_ui_as_parent() {
    // Notice that this test overrides the factory and uses Compose UI as entry point
    // instead of Android Views as the other tests.
    factory = ComposeAndroidRendererFactory.createForComposeUi(testApplication)

    val composeModels = MutableStateFlow<ComposeModel?>(null)
    composeTestRule.runOnUiThread {
      activity.setContent {
        val composeModel = composeModels.collectAsState().value
        if (composeModel != null) {
          factory.getComposeRenderer(composeModel).renderCompose(composeModel)
        }
      }
    }

    repeat(10) {
      composeModels.value = ComposeModel(it)
      composeTestRule.onNodeWithTag("testCompose").assertTextEquals("Compose test: $it")
    }

    assertThat(createdRenderers).isEqualTo(1)
  }

  @Test
  fun a_view_renderer_renders_content_on_screen() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val viewModel = ViewModel(it)
        factory.getRenderer(viewModel).render(viewModel)
        assertThat(activity.testView.text.toString()).isEqualTo("View test: $it")
      }
    }

    assertThat(createdRenderers).isEqualTo(1)
  }

  @Test
  fun a_view_renderer_renders_content_on_screen_in_compose_ui_as_parent() {
    // Notice that this test overrides the factory and uses Compose UI as entry point
    // instead of Android Views as the other tests.
    factory = ComposeAndroidRendererFactory.createForComposeUi(testApplication)

    val viewModels = MutableStateFlow<ViewModel?>(null)
    composeTestRule.runOnUiThread {
      activity.setContent {
        val viewModel = viewModels.collectAsState().value
        if (viewModel != null) {
          factory.getComposeRenderer(viewModel).renderCompose(viewModel)
        }
      }
    }

    repeat(10) {
      viewModels.value = ViewModel(it)

      waitForRenderPass()

      assertThat(activity.testView.text.toString()).isEqualTo("View test: $it")
    }

    assertThat(createdRenderers).isEqualTo(1)
  }

  @Test
  fun a_renderer_cannot_render_android_views_without_a_parent_view_when_compose_is_expected() {
    // Notice that this test overrides the factory and uses Compose UI as entry point
    // instead of Android Views as the other tests.
    factory = ComposeAndroidRendererFactory.createForComposeUi(testApplication)

    composeTestRule.runOnUiThread {
      activity.setContent {
        assertFailure {
            val viewModel = ViewModel(1)
            factory.getRenderer(viewModel).render(viewModel)
          }
          .messageContains(
            "Tried to call render() on an AndroidViewRenderer without a parent view."
          )
      }
    }
  }

  @Test
  fun a_view_renderer_can_embed_a_compose_renderer() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val viewModel = ViewModel(it, composeModel = ComposeModel(it + 1))

        factory.getRenderer(viewModel).render(viewModel)
        assertThat(activity.testView.text.toString()).isEqualTo("View test: $it")
      }

      composeTestRule.onNodeWithTag("testCompose").assertTextEquals("Compose test: ${it + 1}")
    }

    assertThat(createdRenderers).isEqualTo(2)
  }

  @Test
  fun a_view_renderer_can_embed_a_view_renderer() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val viewModel = ViewModel(it, viewModel = ViewModel(it + 1))

        factory.getRenderer(viewModel).render(viewModel)

        val outerTextView =
          activity.contentView.getChildViewGroupAt(0).getChildViewGroupAt(0).getChildAt(0)
            as TextView

        assertThat(outerTextView.text.toString()).isEqualTo("View test: $it")

        val innerTextView =
          activity.contentView
            .getChildViewGroupAt(0)
            .getChildViewGroupAt(0)
            .getChildViewGroupAt(1)
            .getChildAt(0) as TextView

        assertThat(innerTextView.text.toString()).isEqualTo("View test: ${it + 1}")
      }
    }

    assertThat(createdRenderers).isEqualTo(2)
  }

  @Test
  fun a_compose_renderer_can_embed_a_view_renderer() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val composeModel = ComposeModel(it, viewModel = ViewModel(it + 1))
        factory.getRenderer(composeModel).render(composeModel)
      }

      composeTestRule.onNodeWithTag("testCompose").assertTextEquals("Compose test: $it")
      assertThat(activity.testView.text.toString()).isEqualTo("View test: ${it + 1}")
    }

    assertThat(createdRenderers).isEqualTo(2)
  }

  @Test
  fun a_compose_renderer_can_embed_a_view_renderer_in_compose_ui_as_parent() {
    // Notice that this test overrides the factory and uses Compose UI as entry point
    // instead of Android Views as the other tests.
    factory = ComposeAndroidRendererFactory.createForComposeUi(testApplication)

    val composeModels = MutableStateFlow<ComposeModel?>(null)
    composeTestRule.runOnUiThread {
      activity.setContent {
        val composeModel = composeModels.collectAsState().value
        if (composeModel != null) {
          factory.getComposeRenderer(composeModel).renderCompose(composeModel)
        }
      }
    }

    repeat(10) {
      composeModels.value = ComposeModel(it, viewModel = ViewModel(it + 1))

      composeTestRule.onNodeWithTag("testCompose").assertTextEquals("Compose test: $it")
      assertThat(activity.testView.text.toString()).isEqualTo("View test: ${it + 1}")
    }

    assertThat(createdRenderers).isEqualTo(2)
  }

  @Test
  fun a_compose_renderer_can_embed_a_compose_renderer() {
    repeat(10) {
      composeTestRule.runOnUiThread {
        val composeModel = ComposeModel(it, composeModel = ComposeModel(it + 1))
        factory.getRenderer(composeModel).render(composeModel)
      }

      composeTestRule.onNodeWithText("Compose test: $it").assertIsDisplayed()
      composeTestRule
        .onNodeWithText("Compose test: $it")
        .onSiblings()
        .assertAny(hasText("Compose test: ${it + 1}"))
    }

    assertThat(createdRenderers).isEqualTo(2)
  }

  @Test
  fun compose_is_not_initialized_in_the_hierarchy_for_android_view_only() {
    composeTestRule.runOnUiThread {
      // Use two nested Android View renderers.
      val viewModel = ViewModel(1, viewModel = ViewModel(2))
      factory.getRenderer(viewModel).render(viewModel)

      assertThat(activity.testView.text.toString()).isEqualTo("View test: 1")
    }

    assertFailure { composeTestRule.onRoot().assertIsDisplayed() }
      .messageContains("No compose hierarchies found in the app.")
  }

  @Test
  fun android_views_are_not_initialized_in_the_hierarchy_for_compose_ui_only() {
    composeTestRule.runOnUiThread {
      // Use two nested Compose UI renderers.
      val composeModel = ComposeModel(1, composeModel = ComposeModel(2))
      factory.getRenderer(composeModel).render(composeModel)
    }

    composeTestRule.onNodeWithText("Compose test: 1").assertIsDisplayed()

    // This is a ViewRenderer due to the wrapping in the factory.
    //
    // We use the inner renderer, because the parent Renderer is the Android Activity.
    val innerRenderer = factory.getComposeRenderer(ComposeModel::class, rendererId = 1)
    assertThat(innerRenderer).isInstanceOf<ViewRenderer<*>>()

    // No view was initialized
    val viewField = ViewRenderer::class.java.declaredFields.single { it.name == "view" }
    viewField.isAccessible = true
    assertThat(viewField.get(innerRenderer)).isNull()

    // No coroutineScope was initialized was initialized
    val coroutineScopeField =
      ViewRenderer::class.java.declaredFields.single { it.name == "coroutineScope" }
    coroutineScopeField.isAccessible = true
    assertThat(coroutineScopeField.get(innerRenderer)).isNull()

    // The activity field was set.
    val activityField = ViewRenderer::class.java.declaredFields.single { it.name == "activity" }
    activityField.isAccessible = true
    assertThat(activityField.get(innerRenderer)).isSameInstanceAs(activity)
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private val Activity.testView: TextView
    get() = contentView.testView

  private val View.testView: TextView
    get() = findViewWithTag("testView")

  private fun ViewGroup.getChildViewGroupAt(index: Int): ViewGroup = getChildAt(index) as ViewGroup

  private fun waitForRenderPass() {
    // This ensures that rendering finished when Android Views are involved.
    composeTestRule.onNodeWithTag("any").assertDoesNotExist()
  }

  private data class ViewModel(
    val value: Int,
    val composeModel: ComposeModel? = null,
    val viewModel: ViewModel? = null,
  ) : BaseModel

  private data class ComposeModel(
    val value: Int,
    val composeModel: ComposeModel? = null,
    val viewModel: ViewModel? = null,
  ) : BaseModel

  private inner class TestViewRenderer(private val rendererFactory: RendererFactory) :
    ViewRenderer<ViewModel>() {

    private lateinit var container: ViewGroup
    private lateinit var textView: TextView

    init {
      createdRenderers++
    }

    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: ViewModel,
    ): View =
      LinearLayout(activity).also { layout ->
        container = layout
        textView = TextView(activity).also { it.tag = "testView" }

        layout.addView(textView)
      }

    override fun renderModel(model: ViewModel) {
      textView.text = "View test: ${model.value}"

      if (model.composeModel != null) {
        rendererFactory.getRenderer(model.composeModel).render(model.composeModel)
      }
      if (model.viewModel != null) {
        rendererFactory
          .getRenderer(model.viewModel::class, container, rendererId = 1)
          .render(model.viewModel)
      }
    }
  }

  private inner class TestComposeRenderer(private val rendererFactory: RendererFactory) :
    ComposeRenderer<ComposeModel>() {

    init {
      createdRenderers++
    }

    @Composable
    override fun Compose(model: ComposeModel, modifier: Modifier) {
      Column {
        BasicText(text = "Compose test: ${model.value}", modifier = Modifier.testTag("testCompose"))

        if (model.viewModel != null) {
          val renderer = rendererFactory.getComposeRenderer(model.viewModel)
          renderer.renderCompose(model.viewModel)
        }

        if (model.composeModel != null) {
          val renderer = rendererFactory.getComposeRenderer(model.composeModel, rendererId = 1)
          renderer.renderCompose(model.composeModel)
        }
      }
    }
  }

  private inner class TestRendererGraph(private val rendererFactory: () -> RendererFactory) :
    RendererGraph {
    override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
      mapOf(
        ViewModel::class to { TestViewRenderer(rendererFactory()) },
        ComposeModel::class to { TestComposeRenderer(rendererFactory()) },
      )
    override val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
      mapOf(
        ViewModel::class to TestViewRenderer::class,
        ComposeModel::class to TestComposeRenderer::class,
      )
  }
}
