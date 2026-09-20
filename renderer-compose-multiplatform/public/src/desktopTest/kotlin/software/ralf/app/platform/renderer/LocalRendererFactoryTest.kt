package software.ralf.app.platform.renderer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import assertk.assertions.messageContains
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.buildTestScope
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph

@OptIn(ExperimentalTestApi::class)
class LocalRendererFactoryTest {

  @Test
  fun `created and cached renderers provide their factory to nested renderers`() = runTest {
    for (cached in listOf(false, true)) {
      val factory = factory()
      val renderer =
        if (cached) factory.getComposeRenderer(ParentModel::class)
        else factory.createComposeRenderer(ParentModel::class)

      runComposeUiTest {
        val models = MutableStateFlow(ParentModel(ParentModel(TextModel("first"))))
        setContent {
          val model by models.collectAsState()
          renderer.renderCompose(model, Modifier.testTag("child"))
        }

        onNodeWithTag("child").assertTextEquals("default: first")
        models.value = ParentModel(ParentModel(TextModel("second")))
        onNodeWithTag("child").assertTextEquals("default: second")
      }
    }
  }

  @Test
  fun `an explicit factory overrides a renderer factory and is inherited by children`() = runTest {
    val original = factory("original")
    val replacement = factory("replacement")
    val renderer = original.getComposeRenderer(ParentModel::class)

    runComposeUiTest {
      setContent {
        CompositionLocalProvider(LocalRendererFactory provides replacement) {
          renderer.renderCompose(ParentModel(ParentModel(TextModel("child"))))
        }
      }

      onNodeWithText("replacement: child").assertExists()
    }
  }

  @Test
  fun `independent renderer trees use their own factories`() = runTest {
    val first = factory("first")
    val second = factory("second")

    runComposeUiTest {
      setContent {
        Column {
          first.renderCompose(ParentModel(TextModel("child")))
          second.renderCompose(ParentModel(TextModel("child")))
        }
      }

      onNodeWithText("first: child").assertExists()
      onNodeWithText("second: child").assertExists()
    }
  }

  @Test
  fun `render uses the current factory model type and renderer id`() = runTest {
    val created = mutableListOf<Renderer<*>>()
    val first = factory("first", created)
    val second = factory("second", created)
    val factories = MutableStateFlow<RendererFactory>(first)
    val models = MutableStateFlow<BaseModel>(TextModel("child"))
    val ids = MutableStateFlow(0)

    runComposeUiTest {
      setContent {
        val factory by factories.collectAsState()
        val model by models.collectAsState()
        val rendererId by ids.collectAsState()
        CompositionLocalProvider(LocalRendererFactory provides factory) {
          Render(model, Modifier.testTag("child"), rendererId)
        }
      }

      onNodeWithTag("child").assertTextEquals("first: child")
      assertThat(created.size).isEqualTo(1)
      val initialRenderer = created.single()

      models.value = TextModel("updated")
      onNodeWithTag("child").assertTextEquals("first: updated")
      assertThat(created.size).isEqualTo(1)

      ids.value = 1
      onNodeWithTag("child").assertTextEquals("first: updated")
      assertThat(created.size).isEqualTo(2)

      ids.value = 0
      onNodeWithTag("child").assertTextEquals("first: updated")
      assertThat(first.getComposeRenderer(TextModel::class)).isSameInstanceAs(initialRenderer)

      factories.value = second
      onNodeWithTag("child").assertTextEquals("second: updated")
      assertThat(created.size).isEqualTo(3)

      models.value = ParentModel(TextModel("nested"))
      onNodeWithTag("child").assertTextEquals("second: nested")
      assertThat(created.size).isEqualTo(4)
    }
  }

  @Test
  fun `a manually constructed renderer can inherit a provided factory`() = runTest {
    val factory = factory()

    runComposeUiTest {
      setContent {
        CompositionLocalProvider(LocalRendererFactory provides factory) {
          ParentRenderer().renderCompose(ParentModel(TextModel("child")))
        }
      }

      onNodeWithText("default: child").assertExists()
    }
  }

  @Test
  fun `render explains how to supply a missing factory`() {
    assertFailure { runComposeUiTest { setContent { Render(TextModel("child")) } } }
      .all {
        messageContains("No RendererFactory provided.")
        messageContains("provide LocalRendererFactory explicitly.")
      }
  }

  private fun TestScope.factory(
    label: String = "default",
    created: MutableList<Renderer<*>> = mutableListOf(),
  ): ComposeRendererFactory {
    val scope =
      Scope.buildTestScope(this) {
        addMetroDependencyGraph(
          object : RendererGraph.Factory {
            override fun createRendererGraph(factory: RendererFactory): RendererGraph =
              object : RendererGraph {
                override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
                  mapOf(
                    ParentModel::class to { ParentRenderer().also { created += it } },
                    TextModel::class to { TextRenderer(label).also { created += it } },
                  )
                override val modelToRendererMapping:
                  Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                  mapOf(
                    ParentModel::class to ParentRenderer::class,
                    TextModel::class to TextRenderer::class,
                  )
              }
          }
        )
      }
    return ComposeRendererFactory(
      object : RootScopeProvider {
        override val rootScope: Scope = scope
      }
    )
  }

  private data class ParentModel(val child: BaseModel) : BaseModel

  private data class TextModel(val text: String) : BaseModel

  private class ParentRenderer : ComposeRenderer<ParentModel>() {
    @Composable
    override fun Compose(model: ParentModel, modifier: Modifier) {
      Render(model.child, modifier)
    }
  }

  private class TextRenderer(private val label: String) : ComposeRenderer<TextModel>() {
    @Composable
    override fun Compose(model: TextModel, modifier: Modifier) {
      BasicText("$label: ${model.text}", modifier)
    }
  }
}
