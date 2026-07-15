package software.ralf.app.platform.sample.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.test
import software.ralf.app.platform.presenter.template.ModelDelegate
import software.ralf.app.platform.presenter.template.Template
import software.ralf.app.platform.sample.template.SampleAppTemplate.FullScreenTemplate
import software.ralf.app.platform.sample.template.SampleAppTemplatePresenterTest.TestPresenter.Model

class SampleAppTemplatePresenterTest {

  @Test
  fun `the template provided by the injected presenter is returned`() = runTest {
    val trigger = MutableStateFlow<Template?>(null)
    val testPresenter = TestPresenter(trigger)
    val expectedTemplate = FullScreenTemplate(object : BaseModel {})

    SampleAppTemplatePresenter(BackGestureDispatcherPresenter.createNewInstance(), testPresenter)
      .test(this) {
        val defaultFullScreenTemplate = awaitItem() as FullScreenTemplate
        assertThat(defaultFullScreenTemplate.model).isInstanceOf<Model>()

        trigger.value = expectedTemplate
        assertThat(awaitItem()).isSameInstanceAs(expectedTemplate)
      }
  }

  private class TestPresenter(private val trigger: StateFlow<Template?>) :
    MoleculePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      return Model(trigger.collectAsState().value)
    }

    data class Model(private val delegate: Template?) : BaseModel, ModelDelegate {
      override fun delegate(): BaseModel {
        return delegate ?: this
      }
    }
  }
}
