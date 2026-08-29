@file:OptIn(ExperimentalMaterial3Api::class)

package software.ralf.circuit.listdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.age_at_ring_destruction
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.character_list_title
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.no_characters_available
import software.ralf.circuit.listdetail.theme.AppTheme

@Inject
class CharacterListUi : Ui<CharacterListPresenter.State> {
  @Composable
  override fun Content(state: CharacterListPresenter.State, modifier: Modifier) {
    Scaffold(
      modifier = modifier.fillMaxSize().testTag("characterList"),
      topBar = {
        CenterAlignedTopAppBar(title = { Text(stringResource(Res.string.character_list_title)) })
      },
    ) { contentPadding ->
      if (state.characters.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(Res.string.no_characters_available),
            color = AppTheme.colorScheme.onSurfaceVariant,
            style = AppTheme.typography.bodyLarge,
          )
        }
      } else {
        LazyColumn(contentPadding = contentPadding) {
          items(state.characters, key = { it.id }) { character ->
            CharacterItem(
              character = character,
              selected = character.id == state.selectedCharacterId,
              onClick = {
                state.eventSink(CharacterListPresenter.Event.SelectCharacter(character.id))
              },
            )
            HorizontalDivider()
          }
        }
      }
    }
  }

  @Composable
  private fun CharacterItem(character: Character, selected: Boolean, onClick: () -> Unit) {
    CharacterSharedElementScope { transitionScope ->
      ListItem(
        headlineContent = {
          Text(
            text = character.name,
            modifier =
              Modifier.sharedCharacterBounds(
                characterId = character.id,
                element = CharacterSharedElementKey.Element.Name,
                transitionScope = transitionScope,
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTheme.typography.titleMedium,
          )
        },
        supportingContent = {
          Text(
            text =
              stringResource(
                Res.string.age_at_ring_destruction,
                character.ageAtRingDestruction,
              ),
            modifier =
              Modifier.sharedCharacterBounds(
                characterId = character.id,
                element = CharacterSharedElementKey.Element.Age,
                transitionScope = transitionScope,
              ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        },
        leadingContent = {
          CharacterPortraitImage(
            character = character,
            modifier =
              Modifier.size(56.dp)
                .sharedCharacterBounds(
                  characterId = character.id,
                  element = CharacterSharedElementKey.Element.Portrait,
                  transitionScope = transitionScope,
                ),
          )
        },
        colors =
          ListItemDefaults.colors(
            containerColor =
              if (selected) {
                AppTheme.colorScheme.secondaryContainer
              } else {
                AppTheme.colorScheme.surface
              }
          ),
        modifier =
          Modifier.selectable(selected = selected, onClick = onClick)
            .testTag("character-${character.id}"),
      )
    }
  }
}
