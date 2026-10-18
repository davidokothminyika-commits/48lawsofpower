package com.example.a48lawsofpower.ui.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * Shows what a tapped word means.
 *
 * ## Why a bottom sheet and not a dialog
 *
 * The reader taps a word to check a meaning and wants to be back reading immediately. A sheet
 * dismisses with a swipe in the direction the thumb already is, and it leaves a strip of the
 * paragraph visible behind it, so the reader keeps their place. A centred dialog would cover the
 * sentence they are asking about.
 *
 * ## Why a miss says so plainly
 *
 * WordNet is a general lexicon with a 1993 horizon and carries no proper nouns, so a tap on
 * "Machiavelli" or "observance" finds nothing. The honest response is to say the word was not
 * found, and say which dictionary - a bare "not found" invites the reader to think the app is
 * broken, when what has happened is that this particular lexicon does not cover it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySheet(
    word: String,
    entry: Entry?,
    dictionarySize: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
        containerColor = CourtGrayElevated,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                // Bottom padding for the gesture bar: without it the definition sits under the
                // home indicator on gesture-nav devices.
                .padding(bottom = Spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.ifBlank { "—" },
                    style = LawTheme.typography.lawTitle,
                    color = ParchmentWhite,
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                entry?.partOfSpeech?.takeIf { it.isNotBlank() }?.let { pos ->
                    Text(
                        text = pos.spoken(),
                        style = LawTheme.typography.eyebrow,
                        color = SovereignGold,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            if (entry == null) {
                Text(
                    text = "Not in the bundled dictionary.",
                    style = LawTheme.typography.bodyPrimary,
                    color = ParchmentWhite,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    // Naming the source matters: "not found" reads as a bug, "not in WordNet"
                    // reads as a fact about a lexicon that has no proper nouns in it.
                    text = "The offline dictionary is Princeton WordNet 3.1, which covers " +
                        "general English and does not include proper nouns or modern coinages. " +
                        "$dictionarySize words are bundled.",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
            } else {
                Text(
                    text = entry.definition,
                    style = LawTheme.typography.bodyPrimary,
                    color = ParchmentWhite,
                )

                if (entry.example.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SovereignGold.copy(alpha = 0.10f))
                            .padding(Spacing.sm),
                    ) {
                        Box(
                            Modifier
                                .size(width = 2.dp, height = 34.dp)
                                .background(SovereignGold.copy(alpha = 0.5f)),
                        )
                        Spacer(Modifier.size(Spacing.xs))
                        Text(
                            text = entry.example,
                            style = LawTheme.typography.bodySecondary,
                            color = ShadowGray,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HairlineBorder),
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Princeton WordNet 3.1",
                    style = LawTheme.typography.actionLabel,
                    color = ShadowGray,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** WordNet's terse part-of-speech codes, spelled out. */
private fun String.spoken(): String =
    when (this) {
        "n" -> "noun"
        "v" -> "verb"
        "a" -> "adjective"
        "r" -> "adverb"
        "s" -> "adjective"
        else -> split('/').joinToString(" or ") { code ->
            when (code) {
                "n" -> "noun"
                "v" -> "verb"
                "a" -> "adjective"
                "r" -> "adverb"
                "s" -> "adjective"
                else -> code
            }
        }
    }
