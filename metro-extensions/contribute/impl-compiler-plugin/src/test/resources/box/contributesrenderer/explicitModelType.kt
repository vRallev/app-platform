// MODULE: model
// FILE: Model.kt
package com.test

import software.ralf.app.platform.presenter.BaseModel

class Model : BaseModel {
  companion object
}

class Model2 : BaseModel

// FILE: ModelAlias.kt
package com.test.alias

typealias ModelAlias = com.test.Model

// MODULE: app(model)
// FILE: app.kt
package com.test

import com.test.alias.ModelAlias
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererGraph

@ContributesRenderer(ModelAlias::class)
class TestRenderer : Renderer<Model2> {
  override fun render(model: Model2) = Unit
}

@DependencyGraph(AppScope::class)
interface AppGraph

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  if (graph.renderers.keys != setOf(Model::class)) {
    return "FAIL: explicit model type should win, but got keys ${graph.renderers.keys}"
  }
  if (graph.modelToRendererMapping != mapOf(Model::class to TestRenderer::class)) {
    return "FAIL: unexpected modelToRendererMapping ${graph.modelToRendererMapping}"
  }

  return "OK"
}
