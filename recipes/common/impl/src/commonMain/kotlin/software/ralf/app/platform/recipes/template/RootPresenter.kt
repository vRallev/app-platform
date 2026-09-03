package software.ralf.app.platform.recipes.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withCompositionLocal
import me.tatarka.inject.annotations.Inject
import software.ralf.app.platform.presenter.molecule.MoleculePresenter
import software.ralf.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter
import software.ralf.app.platform.presenter.template.toTemplate
import software.ralf.app.platform.recipes.appbar.AppBarConfig
import software.ralf.app.platform.recipes.appbar.AppBarConfigModel
import software.ralf.app.platform.recipes.backstack.CrossSlideBackstackPresenter
import software.ralf.app.platform.recipes.landing.LandingPresenter

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [RecipesAppTemplate]s.
 */
@Inject
class RootPresenter(
  private val landingPresenter: LandingPresenter,
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : MoleculePresenter<Unit, RecipesAppTemplate> {
  @Composable
  override fun present(input: Unit): RecipesAppTemplate {
    return withCompositionLocal(
      LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
    ) {
      val backstackPresenter = remember { CrossSlideBackstackPresenter(landingPresenter) }
      val backstackModel = backstackPresenter.present(Unit)

      backstackModel.toTemplate { model ->
        val appBarConfig =
          if (model is AppBarConfigModel) {
            model.appBarConfig()
          } else {
            AppBarConfig.DEFAULT
          }

        RecipesAppTemplate.FullScreenTemplate(model, appBarConfig)
      }
    }
  }
}
