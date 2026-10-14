package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.ui.annotations.HighlightColor
import com.example.a48lawsofpower.ui.annotations.HighlightStrokes
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * Editor for the note attached to a mark.
 *
 * The quoted passage is shown above the field on purpose. A note typed without seeing what it is
 * attached to is usually a note about the wrong thing, and by the time it is wrong the reader has
 * moved on.
 *
 * Dismissible, and treats an empty note as a cancel rather than saving a blank one - the mark
 * itself already exists and is untouched either way.
 */
@Composable
fun NoteEditorDialog(
    quoted: String,
    lawTitle: String,
    initialNote: String = "",
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var note by remember { mutableStateOf(initialNote) }

    // The quotes are shown as the reader left them, so the editor stays usable if the mark's
    // anchor is empty - which is the normal case for a bookmark or a mark made before the
    // anchor backfill ran.
    val preview = quoted.ifBlank { "No passage captured for this mark." }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CourtGrayElevated)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(Spacing.md),
    ) {
        Column {
            Text(
                text = lawTitle.uppercase(),
                style = LawTheme.typography.eyebrow,
                color = SovereignGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = preview,
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(CourtGray)
                    .padding(Spacing.sm),
            )
            Spacer(Modifier.height(Spacing.md))

            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text("Your note", style = LawTheme.typography.bodySecondary, color = ShadowGray)
                },
                maxLines = 5,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CourtGray,
                    unfocusedContainerColor = CourtGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = ParchmentWhite,
                    unfocusedTextColor = ParchmentWhite,
                    cursorColor = SovereignGold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp),
            )

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cancel",
                    color = ShadowGray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.xs))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Save",
                    color = SovereignGold,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.xs))
                        .clickable { onSave(note) }
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
    }
}

/** The coloured disc that identifies a mark's type or colour in the review list. */
@Composable
fun AnnotationDot(colorId: String, modifier: Modifier = Modifier) {
    val color = HighlightColor.fromId(colorId)
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(HighlightStrokes[color] ?: SovereignGold),
    )
}