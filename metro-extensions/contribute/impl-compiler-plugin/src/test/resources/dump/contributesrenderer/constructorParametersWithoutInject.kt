// RUN_PIPELINE_TILL: BACKEND
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer

class Model : BaseModel

class RendererDependency

class AnotherRendererDependency

@ContributesRenderer
class TestRenderer(
  val dependency: RendererDependency,
  val anotherDependency: AnotherRendererDependency,
) : Renderer<Model> {
  override fun render(model: Model) = Unit
}
