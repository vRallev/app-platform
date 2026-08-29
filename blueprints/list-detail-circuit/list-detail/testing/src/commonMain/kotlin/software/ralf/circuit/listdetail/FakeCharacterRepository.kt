package software.ralf.circuit.listdetail

import kotlinx.coroutines.flow.MutableStateFlow

/** Mutable repository fake for presenter tests and host applications. */
class FakeCharacterRepository(initialCharacters: List<Character> = emptyList()) :
  CharacterRepository {
  override val characters = MutableStateFlow(initialCharacters)

  /** Replaces the character list and immediately notifies active presenters. */
  fun update(characters: List<Character>) {
    this.characters.value = characters
  }
}
