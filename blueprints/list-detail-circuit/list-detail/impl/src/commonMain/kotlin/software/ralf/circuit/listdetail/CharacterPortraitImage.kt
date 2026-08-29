package software.ralf.circuit.listdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.Res
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.aragorn
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.arwen
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.bilbo
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.boromir
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.character_profile_picture
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.elrond
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.eowyn
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.frodo
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.galadriel
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.gandalf
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.gimli
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.gollum
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.legolas
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.merry
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.pippin
import software.ralf.circuit.listdetail.list.detail.`impl`.generated.resources.samwise

// An exhaustive branch for every portrait keeps resource changes compile-time safe.
@Suppress("CyclomaticComplexMethod")
@Composable
internal fun CharacterPortraitImage(
  character: Character,
  modifier: Modifier = Modifier,
  shape: Shape = CircleShape,
) {
  val resource =
    when (character.portrait) {
      CharacterPortrait.FRODO -> Res.drawable.frodo
      CharacterPortrait.SAMWISE -> Res.drawable.samwise
      CharacterPortrait.ARAGORN -> Res.drawable.aragorn
      CharacterPortrait.LEGOLAS -> Res.drawable.legolas
      CharacterPortrait.GIMLI -> Res.drawable.gimli
      CharacterPortrait.GANDALF -> Res.drawable.gandalf
      CharacterPortrait.BOROMIR -> Res.drawable.boromir
      CharacterPortrait.GALADRIEL -> Res.drawable.galadriel
      CharacterPortrait.ELROND -> Res.drawable.elrond
      CharacterPortrait.ARWEN -> Res.drawable.arwen
      CharacterPortrait.BILBO -> Res.drawable.bilbo
      CharacterPortrait.GOLLUM -> Res.drawable.gollum
      CharacterPortrait.EOWYN -> Res.drawable.eowyn
      CharacterPortrait.MERRY -> Res.drawable.merry
      CharacterPortrait.PIPPIN -> Res.drawable.pippin
    }

  Image(
    painter = painterResource(resource),
    contentDescription = stringResource(Res.string.character_profile_picture, character.name),
    contentScale = ContentScale.Crop,
    modifier = modifier.clip(shape),
  )
}
