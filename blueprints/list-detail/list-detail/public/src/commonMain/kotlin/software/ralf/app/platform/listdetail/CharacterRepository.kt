package software.ralf.app.platform.listdetail

import kotlinx.coroutines.flow.StateFlow

/** Observable source of character data consumed by presentation code. */
interface CharacterRepository {
  /** Current characters; updates automatically trigger presenter recomposition. */
  val characters: StateFlow<List<Character>>
}
