package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

class Model : BaseModel

@ContributesRenderer
class TestRenderer private constructor(
  val string: String,
  val marker: String,
) : Renderer<Model> {
  @Inject constructor(string: String) : this(string, "injected")

  override fun render(model: Model) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph {
  @Provides fun provideString(): String = "abc"
}

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  val rendererProvider = graph.renderers.getValue(Model::class)
  val renderer = rendererProvider()
  if (renderer !is TestRenderer) {
    return "FAIL: expected TestRenderer but got $renderer"
  }
  if (renderer.string != "abc") {
    return "FAIL: expected injected string to be abc but got ${renderer.string}"
  }
  if (renderer.marker != "injected") {
    return "FAIL: expected marker to be injected but got ${renderer.marker}"
  }
  if (graph.renderers.keys != setOf(Model::class)) {
    return "FAIL: unexpected renderer keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping != mapOf(Model::class to TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping ${graph.modelToRendererMapping}"
  }

  return "OK"
}
