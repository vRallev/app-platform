package software.ralf.circuit.listdetail

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class CharacterDetailScreen(
  val characterId: String,
  val showBackButton: Boolean = true,
) : Screen
