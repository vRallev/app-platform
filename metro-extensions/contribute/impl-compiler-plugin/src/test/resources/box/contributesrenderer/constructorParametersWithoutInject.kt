package com.test

import dev.zacsweers.metro.BindingContainer
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

class Model : BaseModel

@ContributesRenderer
class TestRenderer(val string: String) : Renderer<Model> {
  override fun render(model: Model) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph {
  @Provides fun provideString(): String = "abc"
}

fun box(): String {
  if (
    TestRenderer.RendererContribution::class.java.getAnnotation(BindingContainer::class.java) ==
      null
  ) {
    return "FAIL: expected RendererContribution to be a BindingContainer"
  }
  if (
    TestRenderer.RendererContribution::class.java.declaredMethods.any {
      it.name == "provideComTestTestRenderer"
    }
  ) {
    return "FAIL: expected constructor provider to be moved off the RendererContribution interface"
  }
  if (
    TestRenderer.RendererContribution.Companion::class.java.declaredMethods.none {
      it.name == "provideComTestTestRenderer"
    }
  ) {
    return "FAIL: expected constructor provider on RendererContribution companion"
  }

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
  if (graph.renderers.keys != setOf(Model::class)) {
    return "FAIL: unexpected renderer keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping != mapOf(Model::class to TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping ${graph.modelToRendererMapping}"
  }

  return "OK"
}
