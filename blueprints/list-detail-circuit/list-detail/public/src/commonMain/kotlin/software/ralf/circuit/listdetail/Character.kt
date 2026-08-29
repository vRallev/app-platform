package software.ralf.circuit.listdetail

/** Immutable character data shared by the list and detail presentations. */
data class Character(
  /** Stable identifier used for selection state and lazy-list item keys. */
  val id: String,
  /** Name shown in the list and detail panes. */
  val name: String,
  /** Exact or approximate age when the One Ring was destroyed. */
  val ageAtRingDestruction: String,
  /** Bundled portrait resource associated with this character. */
  val portrait: CharacterPortrait,
)
