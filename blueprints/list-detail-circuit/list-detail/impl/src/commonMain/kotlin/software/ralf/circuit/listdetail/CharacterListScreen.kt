package software.ralf.circuit.listdetail

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable data class CharacterListScreen(val selectedCharacterId: String? = null) : Screen
