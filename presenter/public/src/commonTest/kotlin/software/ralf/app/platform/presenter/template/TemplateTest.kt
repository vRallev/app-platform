package software.ralf.app.platform.presenter.template

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import software.ralf.app.platform.presenter.BaseModel

class TemplateTest {

  sealed interface TestTemplate : Template {
    data class FullScreen(val model: BaseModel) : TestTemplate
  }

  @Test
  fun `toTemplate returns the inner most template with delegated models`() {
    val delegatedModel = object : BaseModel {}
    val innerTemplate = TestTemplate.FullScreen(delegatedModel)

    class InnerModel : BaseModel, ModelDelegate {
      override fun delegate(): BaseModel = innerTemplate
    }

    class OuterModel : BaseModel, ModelDelegate {
      override fun delegate(): BaseModel = InnerModel()
    }

    val result = OuterModel().toTemplate { throw NotImplementedError() }
    assertThat(result).isSameInstanceAs(innerTemplate)
  }

  @Test
  fun `toTemplate returns the default template`() {
    val model = object : BaseModel {}
    val result = model.toTemplate { TestTemplate.FullScreen(it) }
    assertThat(result).isInstanceOf<TestTemplate.FullScreen>()
  }
}
