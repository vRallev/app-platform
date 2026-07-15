// MODULE: public
// FILE: Template.kt
package com.test

import software.ralf.app.platform.presenter.BaseModel

sealed interface Template : BaseModel {
  data object FullScreen : Template

  data object ListDetail : Template
}

// MODULE: impl(public)
// FILE: TestRenderer.kt
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.Renderer

@ContributesRenderer
class TestRenderer : Renderer<Template> {
  override fun render(model: Template) = Unit
}

// MODULE: app(impl public)
// FILE: app.kt
package com.test

import software.ralf.app.platform.metro.compiler.support.UnusedRendererFactory
import software.ralf.app.platform.renderer.RendererGraph

@DependencyGraph(AppScope::class)
interface AppGraph

fun box(): String {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  val expectedKeys =
    setOf(
      Template::class,
      Template.FullScreen::class,
      Template.ListDetail::class,
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
