package software.ralf.app.platform.renderer

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.scope.RootScopeProvider
import software.ralf.app.platform.scope.Scope
import software.ralf.app.platform.scope.di.addKotlinInjectComponent
import software.ralf.app.platform.scope.di.metro.addMetroDependencyGraph

class BaseRendererFactoryTest {

  @Test
  fun `creating a renderer without mapping throws an error`() {
    val exception =
      assertFailsWith<Exception> {
        rendererFactory(
            kotlinInjectRenderers = emptyMap(),
            kotlinInjectModelToRendererMapping = emptyMap(),
            metroRenderers = emptyMap(),
            metroModelToRendererMapping = emptyMap(),
          )
          .getRenderer(TestModel(1))
      }

    // Transform the message, because it's slightly different on iOS than on
    // Android and Desktop.
    val message =
      exception.message?.replace('\$', '.')?.replace(" (Kotlin reflection is not available)", "")

    val errorMessage =
      "No renderer was provided for class " +
        "software.ralf.app.platform.renderer.BaseRendererFactoryTest.TestModel. " +
        "Did you add @ContributesRenderer?"

    assertThat(message).isEqualTo(errorMessage)
  }

  @Test
  fun `the createRenderer function always creates new renderers`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.createRenderer(model)).isNotSameInstanceAs(factory.createRenderer(model))
  }

  @Test
  fun `the createRenderer function always creates new renderers after a renderer was cached`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.getRenderer(model)).isNotSameInstanceAs(factory.createRenderer(model))
  }

  @Test
  fun `the getRenderer function caches renderers based on type`() {
    val factory = rendererFactory()
    val model1 = TestModel(1)
    val model2 = TestModel(2)

    assertThat(factory.getRenderer(model1)).isSameInstanceAs(factory.getRenderer(model2))
  }

  @Test
  fun `the getRenderer function caches renderers based on type and id`() {
    val factory = rendererFactory()
    val model1 = TestModel(1)
    val model2 = TestModel(2)

    assertThat(factory.getRenderer(model1, rendererId = 1))
      .isSameInstanceAs(factory.getRenderer(model2, rendererId = 1))
  }

  @Test
  fun `the getRenderer function returns a new renderer on ID mismatch`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.getRenderer(model, rendererId = 1))
      .isNotSameInstanceAs(factory.getRenderer(model, rendererId = 2))
  }

  @Test
  fun `the getRenderer function caches renderers based on the sealed type`() {
    val factory = rendererFactory()
    val model1 = SealedTestModel.Model1(1)
    val model2 = SealedTestModel.Model2(1)

    assertThat(factory.getRenderer(model1)).isSameInstanceAs(factory.getRenderer(model2))
  }

  @Test
  fun `renderers can be provided by kotlin-inject alone`() {
    val factory = rendererFactory(useKotlinInject = true, useMetro = false)
    val model = TestModel(1)

    assertThat(factory.getRenderer(model)).isSameInstanceAs(factory.getRenderer(model))

    assertFailsWith<Exception> { factory.getRenderer(SealedTestModel.Model1(1)) }
  }

  @Test
  fun `renderers can be provided by Metro alone`() {
    val factory = rendererFactory(useKotlinInject = false, useMetro = true)
    val model = SealedTestModel.Model1(1)

    assertThat(factory.getRenderer(model)).isSameInstanceAs(factory.getRenderer(model))

    assertFailsWith<Exception> { factory.getRenderer(TestModel(1)) }
  }

  @Test
  fun `renderers can be provided by kotlin-inject and Metro together`() {
    val factory = rendererFactory(useKotlinInject = true, useMetro = true)
    val model1 = TestModel(1)
    val model2 = SealedTestModel.Model1(1)

    assertThat(factory.getRenderer(model1)).isSameInstanceAs(factory.getRenderer(model1))
    assertThat(factory.getRenderer(model2)).isSameInstanceAs(factory.getRenderer(model2))
  }

  private fun rendererFactory(
    kotlinInjectRenderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
      mapOf(TestModel::class to { TestRenderer() }),
    kotlinInjectModelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
      mapOf(TestModel::class to TestRenderer::class),
    metroRenderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
      mapOf(
        SealedTestModel::class to { SealedTestRenderer() },
        SealedTestModel.Model1::class to { SealedTestRenderer() },
        SealedTestModel.Model2::class to { SealedTestRenderer() },
      ),
    metroModelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
      mapOf(
        SealedTestModel::class to SealedTestRenderer::class,
        SealedTestModel.Model1::class to SealedTestRenderer::class,
        SealedTestModel.Model2::class to SealedTestRenderer::class,
      ),
    useKotlinInject: Boolean = true,
    useMetro: Boolean = true,
  ): RendererFactory =
    BaseRendererFactory(
      rootScopeProvider =
        object : RootScopeProvider {
          override val rootScope: Scope = Scope.buildRootScope {
            if (useKotlinInject) {
              addKotlinInjectComponent(
                object : RendererComponent.Parent {
                  override fun rendererComponent(factory: RendererFactory): RendererComponent =
                    object : RendererComponent {
                      override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
                        kotlinInjectRenderers

                      override val modelToRendererMapping:
                        Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                        kotlinInjectModelToRendererMapping
                    }
                }
              )
            }

            if (useMetro) {
              addMetroDependencyGraph(
                object : RendererGraph.Factory {
                  override fun createRendererGraph(factory: RendererFactory): RendererGraph =
                    object : RendererGraph {
                      override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
                        metroRenderers
                      override val modelToRendererMapping:
                        Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                        metroModelToRendererMapping
                    }
                }
              )
            }
          }
        }
    )

  private data class TestModel(val value: Int) : BaseModel

  private class TestRenderer : Renderer<TestModel> {
    override fun render(model: TestModel) = Unit
  }

  private sealed interface SealedTestModel : BaseModel {
    data class Model1(val value: Int) : SealedTestModel

    data class Model2(val value: Int) : SealedTestModel
  }

  private class SealedTestRenderer : Renderer<SealedTestModel> {
    override fun render(model: SealedTestModel) = Unit
  }
}
