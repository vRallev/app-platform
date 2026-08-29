package software.ralf.circuit.listdetail

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoSet(AppScope::class)
class ListDetailPresenterFactory(
  private val listDetailPresenter: ListDetailPresenter,
  private val listPresenterFactory: CharacterListPresenter.Factory,
  private val detailPresenterFactory: CharacterDetailPresenter.Factory,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? =
    when (screen) {
      ListDetailScreen -> listDetailPresenter
      is CharacterListScreen -> listPresenterFactory.create(screen, navigator)
      is CharacterDetailScreen -> detailPresenterFactory.create(screen, navigator)
      else -> null
    }
}
