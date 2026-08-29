@file:OptIn(ExperimentalMaterial3Api::class)

package software.ralf.circuit.listdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.age_at_ring_destruction
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.back
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.character_detail_title
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.character_not_available
import software.ralf.circuit.listdetail.theme.AppTheme

@Inject
class CharacterDetailUi : Ui<CharacterDetailPresenter.State> {
  @Composable
  override fun Content(state: CharacterDetailPresenter.State, modifier: Modifier) {
    Scaffold(
      modifier = modifier.fillMaxSize().testTag("characterDetail"),
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text(stringResource(Res.string.character_detail_title)) },
          navigationIcon = {
            if (state.showBackButton) {
              IconButton(
                onClick = { state.eventSink(CharacterDetailPresenter.Event.Back) },
                modifier = Modifier.testTag("detailBack"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(Res.string.back),
                )
              }
            }
          },
        )
      },
    ) { contentPadding ->
      when (val detail = state.state) {
        is CharacterDetailPresenter.DetailState.Available ->
          CharacterContent(character = detail.character, contentPadding = contentPadding)
        is CharacterDetailPresenter.DetailState.Missing ->
          Box(
            modifier =
              Modifier.fillMaxSize().padding(contentPadding).testTag("characterDetailMissing"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(Res.string.character_not_available),
              color = AppTheme.colorScheme.onSurfaceVariant,
              style = AppTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
            )
          }
      }
    }
  }

  @Composable
  private fun CharacterContent(character: Character, contentPadding: PaddingValues) {
    CharacterSharedElementScope { transitionScope ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        CharacterPortraitImage(
          character = character,
          modifier =
            Modifier.sizeIn(maxWidth = 384.dp, maxHeight = 384.dp)
              .fillMaxWidth()
              .aspectRatio(1f)
              .sharedCharacterBounds(
                characterId = character.id,
                element = CharacterSharedElementKey.Element.Portrait,
                transitionScope = transitionScope,
              ),
          shape = RoundedCornerShape(24.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
          text = character.name,
          modifier =
            Modifier.testTag("characterDetailName")
              .sharedCharacterBounds(
                characterId = character.id,
                element = CharacterSharedElementKey.Element.Name,
                transitionScope = transitionScope,
              ),
          style = AppTheme.typography.headlineLarge,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text =
            stringResource(
              Res.string.age_at_ring_destruction,
              character.ageAtRingDestruction,
            ),
          modifier =
            Modifier.testTag("characterDetailAge")
              .sharedCharacterBounds(
                characterId = character.id,
                element = CharacterSharedElementKey.Element.Age,
                transitionScope = transitionScope,
              ),
          color = AppTheme.colorScheme.onSurfaceVariant,
          style = AppTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
