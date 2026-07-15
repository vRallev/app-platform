package software.ralf.app.platform.sample.template

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresenters
import software.ralf.app.platform.renderer.ComposeRenderer
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer
import software.ralf.app.platform.sample.template.animation.LocalAnimatedVisibilityScope
import software.ralf.app.platform.sample.template.animation.LocalSharedTransitionScope

/**
 * A Compose renderer implementation for templates used in the sample application.
 *
 * [rendererFactory] is used to get the [Renderer] for the [BaseModel] wrapped in the template.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@ContributesRenderer
class ComposeSampleAppTemplateRenderer(
  private val rendererFactory: RendererFactory,
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : ComposeRenderer<SampleAppTemplate>() {

  @Composable
  override fun Compose(model: SampleAppTemplate, modifier: Modifier) {
    backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

    Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
      // Wrap all the the UI in a SharedTransitionLayout and AnimatedContent to support
      // shared element transitions across template updates. The scopes are exposed through
      // composition locals as suggested here:
      // https://developer.android.com/develop/ui/compose/animation/shared-elements#understand-scopes
      SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
          AnimatedContent(
            targetState = model,
            label = "Top level AnimatedContent",
            contentKey = { template ->
              // Use the key from AnimationContentKey as indicator when content has changed
              // that needs to be animated. If this key is doesn't change (the default behavior),
              // then no animation occurs.
              template.contentKey
            },
          ) { template ->
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
              when (template) {
                is SampleAppTemplate.FullScreenTemplate -> FullScreen(template)
                is SampleAppTemplate.ListDetailTemplate -> ListDetail(template)
              }
            }
          }
        }
      }
    }
  }

  @Composable
  private fun FullScreen(template: SampleAppTemplate.FullScreenTemplate) {
    val renderer = rendererFactory.getComposeRenderer(template.model)
    renderer.renderCompose(template.model)
  }

  @Composable
  private fun ListDetail(template: SampleAppTemplate.ListDetailTemplate) {
    Row {
      Column(Modifier.weight(1f)) {
        rendererFactory.getComposeRenderer(template.list).renderCompose(template.list)
      }
      Column(Modifier.weight(2f)) {
        rendererFactory.getComposeRenderer(template.detail).renderCompose(template.detail)
      }
    }
  }
}
