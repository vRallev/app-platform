package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

interface Presenter {
  sealed interface Model : BaseModel {
    data object Model1 : Model

    data object Model2 : Model
  }
}

@ContributesRenderer(includeSealedSubtypes = false)
class TestRenderer : Renderer<Presenter.Model> {
  override fun render(model: Presenter.Model) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  val renderer = graph.renderers.getValue(Presenter.Model::class).invoke()
  if (renderer !is TestRenderer) {
    return "FAIL: expected TestRenderer but got $renderer"
  }
  if (graph.renderers.keys != setOf(Presenter.Model::class)) {
    return "FAIL: unexpected renderer keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping != mapOf(Presenter.Model::class to TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping ${graph.modelToRendererMapping}"
  }

  return "OK"
}
