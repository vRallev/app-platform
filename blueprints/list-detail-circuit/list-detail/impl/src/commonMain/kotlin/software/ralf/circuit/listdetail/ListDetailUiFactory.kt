package software.ralf.circuit.listdetail

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoSet(AppScope::class)
class ListDetailUiFactory(
  private val listDetailUi: ListDetailUi,
  private val listUi: CharacterListUi,
  private val detailUi: CharacterDetailUi,
) : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? =
    when (screen) {
      ListDetailScreen -> listDetailUi
      is CharacterListScreen -> listUi
      is CharacterDetailScreen -> detailUi
      else -> null
    }
}
