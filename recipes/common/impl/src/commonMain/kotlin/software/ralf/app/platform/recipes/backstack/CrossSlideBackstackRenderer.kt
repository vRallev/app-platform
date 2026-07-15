@file:OptIn(ExperimentalAppPlatform::class)
@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.ralf.app.platform.recipes.backstack

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.ExperimentalAppPlatform
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.getComposeRenderer

@Inject
@ContributesRenderer
class CrossSlideBackstackRenderer(private val rendererFactory: RendererFactory) :
  PresenterBackstackRenderer<CrossSlideBackstackPresenter.Model>() {
  @Composable
  override fun PresenterNavDisplay(
    backstack: List<Int>,
    onBack: () -> Unit,
    entryProvider: (Int) -> NavEntry<Int>,
    modifier: Modifier,
  ) {
    NavDisplay(
      backStack = backstack,
      onBack = onBack,
      entryProvider = entryProvider,
      modifier = modifier,
      transitionSpec = { crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Left) },
      popTransitionSpec = {
        crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Right)
      },
      predictivePopTransitionSpec = {
        crossSlideTransition(AnimatedContentTransitionScope.SlideDirection.Right)
      },
    )
  }

  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    rendererFactory.getComposeRenderer(model).renderCompose(model)
  }

  private fun AnimatedContentTransitionScope<Scene<Int>>.crossSlideTransition(
    direction: AnimatedContentTransitionScope.SlideDirection
  ): ContentTransform {
    return slideIntoContainer(direction, tween()) togetherWith
      slideOutOfContainer(direction, tween())
  }
}
