package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.ui.annotations.HighlightColor
import com.example.a48lawsofpower.ui.annotations.HighlightStrokes
import com.example.a48lawsofpower.ui.annotations.buildAnnotatedReaderText
import com.example.a48lawsofpower.ui.annotations.resolvePaintableSpans
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * Reader prose that supports selecting a passage with a long-press and marking it.
 *
 * ## Why this is two stacked text layers
 *
 * The app needs custom background spans (the highlights) *and* a readable selection. This
 * Compose version cannot do both in one widget: `SelectionContainer` gives selection but exposes
 * no handle on its `SelectionManager`, and `BasicTextField`'s `TextFieldState` only exposes
 * `addAnnotation`, which is internal - so spans cannot be applied to selectable text.
 *
 * So the two jobs are split by layer:
 *
 *  - a plain [Text] paints the marks and is the only visible text
 *  - a transparent [BasicTextField] sits on top carrying the same text, invisible, purely to
 *    capture the selection
 *
 * Both are given the *same* [TextStyle] instance and the same width, and the field's decoration
 * box is stripped, because the selection handles are positioned by the field's own layout. If the
 * two disagree by even a pixel the handles sit on the wrong words. That coupling is the price of
 * this approach and the reason it is commented so loudly.
 *
 * ## What this costs
 *
 * A field over a scrollable region can swallow drags. [readOnly] keeps it from claiming
 * keystrokes but not necessarily gestures, so this is verified on-device rather than assumed -
 * see the scroll test in the release checklist.
 */
@Composable
fun AnnotatableText(
    text: String,
    annotations: List<AnnotationEntity>,
    onSelectionChanged: (start: Int, end: Int) -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val spans = remember(text, annotations) { resolvePaintableSpans(text, annotations) }
    val painted = remember(text, spans) { buildAnnotatedReaderText(text, spans) }

    val fieldState = rememberTextFieldState(initialText = text)

    val selection = fieldState.selection
    LaunchedEffect(selection) {
        onSelectionChanged(selection.min, selection.max)
    }

    Box(modifier = modifier) {
        Text(
            text = painted,
            style = style,
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )

        // Two ambients, both scoped to the capture layer:
        //  - selection colours, because BasicTextField in this version has no selectionColors
        //    parameter. Without this the handles render in the theme's primary, which is now a
        //    dark crimson and reads as an error state rather than a selection.
        //  - a no-op text toolbar. The platform toolbar offers Copy / Select all / Read aloud
        //    and cannot be given app actions, so it is suppressed and replaced by
        //    [AnnotationActionBar].
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = SovereignGold,
                backgroundColor = SovereignGold.copy(alpha = 0.28f),
            ),
            LocalTextToolbar provides NoOpTextToolbar,
        ) {
            BasicTextField(
                state = fieldState,
                enabled = true,
                // Read-only so the field cannot claim keystrokes or accept edits; selection and the
                // long-press gesture still work, which is the entire reason it is here.
                readOnly = true,
                // Invisible glyphs, visible selection. The colour is what this layer draws; the
                // Text underneath is what the reader actually reads.
                textStyle = style.copy(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * The action bar that replaces the platform selection toolbar: colour swatches, note, cancel.
 *
 * It is drawn here rather than by the system because the stock toolbar cannot be given app
 * actions, and on a 720px screen two floating bars over the same selection is worse than one.
 */
@Composable
fun AnnotationActionBar(
    selectedColor: HighlightColor,
    onColorChange: (HighlightColor) -> Unit,
    onHighlight: () -> Unit,
    onAddNote: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Two rows rather than one.
    //
    // A 360dp bar cannot hold five 26dp swatches plus four text actions: the single-row version
    // measured out near 380dp, so the last action was being clipped on a 720px screen - the same
    // class of bug as the truncated tab labels.
    //
    // Bookmark is on its own row because it is not a passage action. A bookmark flags a law or a
    // section, so it does not depend on the selection the other three do.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.md))
            .background(CourtGrayElevated)
            .border(1.dp, HairlineBorder, RoundedCornerShape(Spacing.md))
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HighlightColor.entries.forEach { option ->
                val stroke = HighlightStrokes[option] ?: SovereignGold
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(stroke)
                        .border(
                            width = if (option == selectedColor) 2.dp else 0.dp,
                            color = if (option == selectedColor) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(13.dp),
                        )
                        .clickable { onColorChange(option) },
                )
                Spacer(Modifier.width(Spacing.xs))
            }
            Spacer(Modifier.weight(1f))
            // Naming the colour makes the swatches mean something before anything is marked,
            // which is the whole point of colour-coding them by intent.
            Text(
                text = selectedColor.label,
                color = ShadowGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = Spacing.xs),
            )
        }

        Spacer(Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarAction("Note", onAddNote)
            BarAction("Bookmark", onToggleBookmark)
            BarAction("Cancel", onCancel, tint = ShadowGray)
            BarAction("Highlight", onHighlight, tint = SovereignGold)
        }
    }
}

@Composable
private fun BarAction(label: String, onClick: () -> Unit, tint: Color = ParchmentWhite) {
    Text(
        text = label,
        color = tint,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.xs))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
    )
}
/**
 * A toolbar that draws nothing.
 *
 * In this Compose version `LocalTextToolbar` holds a [TextToolbar] rather than the older
 * `(TextToolbar) -> Unit` callback, so suppressing the platform toolbar means providing a real
 * implementation whose every method is a no-op. The platform toolbar offers Copy / Select all /
 * Read aloud and cannot be given app actions; [AnnotationActionBar] replaces it.
 */
private object NoOpTextToolbar : TextToolbar {
    override val status: androidx.compose.ui.platform.TextToolbarStatus
        get() = androidx.compose.ui.platform.TextToolbarStatus.Hidden

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}
