// RUN_PIPELINE_TILL: BACKEND
// DUMP_KT_IR
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer

class Model : BaseModel

@ContributesRenderer
class TestRenderer : Renderer<Model> {
  override fun render(model: Model) = Unit
}
