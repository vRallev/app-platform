package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

interface Presenter {
  sealed interface Model : BaseModel {
    sealed interface Inner : Model {
      data object Model1 : Inner

      data object Model2 : Inner
    }

    data object Model2 : Model

    class OtherSubclass
  }
}

@ContributesRenderer
class TestRenderer : Renderer<Presenter.Model> {
  override fun render(model: Presenter.Model) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  val expectedKeys =
    setOf(
      Presenter.Model::class,
      Presenter.Model.Inner::class,
      Presenter.Model.Inner.Model1::class,
      Presenter.Model.Inner.Model2::class,
      Presenter.Model.Model2::class,
    )
  if (graph.renderers.keys != expectedKeys) {
    return "FAIL: unexpected renderer keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping.keys != expectedKeys) {
    return "FAIL: unexpected modelToRendererMapping keys ${graph.modelToRendererMapping.keys}"
  }
  if (graph.modelToRendererMapping.values.toSet() != setOf(TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping values ${graph.modelToRendererMapping.values}"
  }
  for (key in expectedKeys) {
    val renderer = graph.renderers.getValue(key).invoke()
    if (renderer !is TestRenderer) {
      return "FAIL: expected TestRenderer for $key but got $renderer"
    }
  }

  return "OK"
}
