// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererScope

class Model : BaseModel

<!CONTRIBUTES_RENDERER_ERROR!>@ContributesRenderer<!>
@SingleIn(RendererScope::class)
@Inject
class TestRenderer(@Suppress("unused") val string: String) : Renderer<Model> {
  override fun render(model: Model) = Unit
}
