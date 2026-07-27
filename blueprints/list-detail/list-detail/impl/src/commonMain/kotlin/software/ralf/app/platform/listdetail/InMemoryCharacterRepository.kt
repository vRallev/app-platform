package software.ralf.app.platform.listdetail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped repository containing deterministic sample data.
 *
 * Production applications can replace this binding with a database or network-backed implementation
 * without changing the presenters.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InMemoryCharacterRepository : CharacterRepository {
  private val mutableCharacters = MutableStateFlow(createCharacters())

  override val characters: StateFlow<List<Character>> = mutableCharacters.asStateFlow()

  private fun createCharacters(): List<Character> {
    return listOf(
      Character(
        id = "frodo",
        name = "Frodo Baggins",
        ageAtRingDestruction = "50",
        portrait = CharacterPortrait.FRODO,
      ),
      Character(
        id = "samwise",
        name = "Samwise Gamgee",
        ageAtRingDestruction = "38",
        portrait = CharacterPortrait.SAMWISE,
      ),
      Character(
        id = "aragorn",
        name = "Aragorn",
        ageAtRingDestruction = "88",
        portrait = CharacterPortrait.ARAGORN,
      ),
      Character(
        id = "legolas",
        name = "Legolas",
        ageAtRingDestruction = "Unknown",
        portrait = CharacterPortrait.LEGOLAS,
      ),
      Character(
        id = "gimli",
        name = "Gimli",
        ageAtRingDestruction = "140",
        portrait = CharacterPortrait.GIMLI,
      ),
      Character(
        id = "gandalf",
        name = "Gandalf",
        ageAtRingDestruction = "Over 2,000",
        portrait = CharacterPortrait.GANDALF,
      ),
      Character(
        id = "boromir",
        name = "Boromir",
        ageAtRingDestruction = "41",
        portrait = CharacterPortrait.BOROMIR,
      ),
      Character(
        id = "galadriel",
        name = "Galadriel",
        ageAtRingDestruction = "About 8,372",
        portrait = CharacterPortrait.GALADRIEL,
      ),
      Character(
        id = "elrond",
        name = "Elrond",
        ageAtRingDestruction = "About 6,518",
        portrait = CharacterPortrait.ELROND,
      ),
      Character(
        id = "arwen",
        name = "Arwen",
        ageAtRingDestruction = "2,778",
        portrait = CharacterPortrait.ARWEN,
      ),
      Character(
        id = "bilbo",
        name = "Bilbo Baggins",
        ageAtRingDestruction = "128",
        portrait = CharacterPortrait.BILBO,
      ),
      Character(
        id = "gollum",
        name = "Gollum",
        ageAtRingDestruction = "About 589",
        portrait = CharacterPortrait.GOLLUM,
      ),
      Character(
        id = "eowyn",
        name = "Éowyn",
        ageAtRingDestruction = "24",
        portrait = CharacterPortrait.EOWYN,
      ),
      Character(
        id = "merry",
        name = "Meriadoc “Merry” Brandybuck",
        ageAtRingDestruction = "36",
        portrait = CharacterPortrait.MERRY,
      ),
      Character(
        id = "pippin",
        name = "Peregrin “Pippin” Took",
        ageAtRingDestruction = "28",
        portrait = CharacterPortrait.PIPPIN,
      ),
    )
  }
}
