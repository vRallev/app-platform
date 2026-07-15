// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer

class Model1 : BaseModel

class Model2 : BaseModel

interface OtherRenderer<S : BaseModel, T : BaseModel> : Renderer<S>

<!CONTRIBUTES_RENDERER_ERROR!>@ContributesRenderer<!>
class TestRenderer : OtherRenderer<Model1, Model2> {
  override fun render(model: Model1) = Unit
}
