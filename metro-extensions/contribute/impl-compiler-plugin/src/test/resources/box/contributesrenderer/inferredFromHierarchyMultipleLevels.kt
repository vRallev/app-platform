package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

class Model : BaseModel

interface OtherRenderer<S : Any, T : BaseModel, U : CharSequence> : Renderer<T>

interface OtherRenderer2<S : BaseModel, T : Any> : OtherRenderer<T, S, String>

interface OtherRenderer3 : OtherRenderer2<Model, Any>

interface OtherRenderer4 : OtherRenderer3

@ContributesRenderer
class TestRenderer : OtherRenderer4 {
  override fun render(model: Model) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  val renderer = graph.renderers.getValue(Model::class).invoke()
  if (renderer !is TestRenderer) {
    return "FAIL: expected TestRenderer but got $renderer"
  }

  if (graph.renderers.keys != setOf(Model::class)) {
    return "FAIL: unexpected renderer keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping != mapOf(Model::class to TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping ${graph.modelToRendererMapping}"
  }

  return "OK"
}
