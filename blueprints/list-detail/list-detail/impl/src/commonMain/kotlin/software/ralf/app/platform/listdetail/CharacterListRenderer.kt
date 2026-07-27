@file:OptIn(ExperimentalMaterial3Api::class)

package software.ralf.app.platform.listdetail

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
import org.jetbrains.compose.resources.stringResource
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.age_at_ring_destruction
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.character_list_title
import software.ralf.app.platform.listdetail.list.detail.`impl`.generated.resources.no_characters_available
import software.ralf.app.platform.listdetail.theme.AppTheme
import software.ralf.app.platform.renderer.ComposeRenderer

/** Material 3 renderer for the selectable character list and its empty state. */
@ContributesRenderer
class CharacterListRenderer : ComposeRenderer<CharacterListPresenter.Model>() {
  @Composable
  override fun Compose(model: CharacterListPresenter.Model, modifier: Modifier) {
    Scaffold(
      modifier = modifier.fillMaxSize().testTag("characterList"),
      topBar = {
        CenterAlignedTopAppBar(title = { Text(stringResource(Res.string.character_list_title)) })
      },
    ) { contentPadding ->
      if (model.characters.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(Res.string.no_characters_available),
            color = AppTheme.colorScheme.onSurfaceVariant,
            style = AppTheme.typography.bodyLarge,
          )
        }
      } else {
        LazyColumn(contentPadding = contentPadding) {
          items(model.characters, key = { it.id }) { character ->
            CharacterItem(
              character = character,
              selected = character.id == model.selectedCharacterId,
              onClick = { model.onCharacterSelected(character) },
            )
            HorizontalDivider()
          }
        }
      }
    }
  }

  @Composable
  private fun CharacterItem(character: Character, selected: Boolean, onClick: () -> Unit) {
    ListItem(
      headlineContent = {
        Text(
          text = character.name,
          modifier =
            Modifier.sharedCharacterBounds(
              characterId = character.id,
              element = CharacterSharedElementKey.Element.Name,
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
