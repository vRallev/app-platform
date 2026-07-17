package software.ralf.app.platform.sample.template

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.template.AndroidTemplateRenderer
import software.ralf.app.platform.sample.templates.impl.R

/** An Android renderer implementation for templates used in the sample application. */
// This implementation is disabled and we always use ComposeSampleAppTemplateRenderer.
// The Android sample app could use this renderer if desired.
// @ContributesRenderer
class AndroidSampleAppTemplateRenderer(rendererFactory: RendererFactory) :
  AndroidTemplateRenderer<SampleAppTemplate>(rendererFactory) {
  private lateinit var fullScreenContainer: Container
  private lateinit var listContainer: Container
  private lateinit var detailContainer: Container

  override fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: SampleAppTemplate,
  ): View {
    return layoutInflater.inflate(R.layout.sample_app_template_root, parent, false).also {
      fullScreenContainer = Container(activity, it.findViewById(R.id.full_screen_container), null)

      it.findViewById<ViewGroup>(R.id.list).apply {
        this.clipToOutline = true
        this.elevation = 24f
        listContainer = Container(activity, this, null)
      }

      detailContainer = Container(activity, it.findViewById(R.id.detail), null)
    }
  }

  override fun renderModel(model: SampleAppTemplate) {
    when (model) {
      is SampleAppTemplate.FullScreenTemplate -> {
        renderFullScreenTemplate(model)
      }

      is SampleAppTemplate.ListDetailTemplate -> {
        renderListDetailTemplate(model)
      }
    }
  }

  private fun renderFullScreenTemplate(template: SampleAppTemplate.FullScreenTemplate) {
    listContainer.reset()
    detailContainer.reset()

    fullScreenContainer.renderModel(template.model)
  }

  private fun renderListDetailTemplate(template: SampleAppTemplate.ListDetailTemplate) {
    fullScreenContainer.reset()

    listContainer.renderModel(template.list)
    detailContainer.renderModel(template.detail)
  }
}
