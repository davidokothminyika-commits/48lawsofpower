package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.LawOverview
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.TogglePill
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/** The three states the library can show a law in. */
enum class LibraryFilter(val label: String) {
    All("All"),
    Saved("Saved"),
    Mastered("Mastered"),
}

/**
 * The Library: the reader's own collection, assembled from the laws they saved and the ones they
 * finished.
 *
 * A browsing surface, so it reads on whichever family is active. It exists because "My Strategies"
 * answers only one of the two questions a returning reader has - *what did I keep?* and *what have
 * I actually finished?* - and the second one has had no home until now.
 */
@Composable
fun LibraryScreen(
    laws: List<LawOverview>,
    filter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit,
    annotatedLawIds: Set<Int>,
    onLawClick: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptics = rememberHaptics()

    val visible = when (filter) {
        LibraryFilter.All -> laws
        LibraryFilter.Saved -> laws.filter { it.bookmarked }
        LibraryFilter.Mastered -> laws.filter { it.read }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The header is outside the scrolling list, so it has to clear the status bar
                // itself. Without this the title renders under the clock and the StatusBarScrim
                // clips its ascenders.
                .statusBarsPadding()
                .padding(
                    top = Spacing.lg,
                    start = Spacing.md,
                    end = Spacing.md,
                    bottom = Spacing.sm,
                ),
        ) {
            Text(
                text = "LIBRARY",
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Your Collection",
                style = LawTheme.typography.lawNumber.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = ParchmentWhite,
            )
            Spacer(Modifier.height(Spacing.md))
            Hairline(color = SovereignGold)
            Spacer(Modifier.height(Spacing.sm))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                LibraryFilter.entries.forEach { entry ->
                    TogglePill(
                        label = entry.label,
                        selected = entry == filter,
                        onClick = {
                            haptics.selection()
                            onFilterChange(entry)
                        },
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            EmptyState(
                title = emptyTitle(filter),
                body = emptyBody(filter),
                modifier = Modifier.padding(top = Spacing.lg),
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.sm,
                    // The header above is a sibling, not a list item, so the list's own padding has
                    // to clear the bottom bar the Scaffold installed.
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xl,
                ),
            ) {
                items(visible.size, key = { visible[it].id }) { index ->
                    val law = visible[index]
                    LibraryRow(
                        law = law,
                        annotated = law.id in annotatedLawIds,
                        onClick = { onLawClick(law.lawNumber) },
                        onToggleBookmark = { onToggleBookmark(law.lawNumber) },
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                }
            }
        }
    }
}

/**
 * A library row. Lighter than a dossier card - this is a reference list, not an invitation to
 * browse - but it still shows the saved and marked states, because those are the reason the law is
 * in the list at all.
 */
@Composable
private fun LibraryRow(
    law: LawOverview,
    annotated: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = law.lawNumber.toString().padStart(2, '0'),
                style = LawTheme.typography.lawIndex,
                color = ShadowGray,
                modifier = Modifier.width(28.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = law.title,
                style = LawTheme.typography.lawTitle.copy(fontSize = 17.sp, lineHeight = 23.sp),
                color = ParchmentWhite,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.padding(start = 28.dp + Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (law.read) {
                TinyState(Icons.Outlined.CheckCircleOutline, "Read", ObservanceGreen)
            }
            if (annotated) {
                TinyState(Icons.Outlined.EditNote, "Annotated", SovereignGold)
            }
            Spacer(Modifier.weight(1f))
            TinyState(
                icon = Icons.Outlined.Bookmark,
                label = if (law.bookmarked) "Saved" else "Save",
                tint = if (law.bookmarked) SovereignGold else ShadowGray,
                onClick = onToggleBookmark,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Hairline(color = HairlineBorder)
    }
}

@Composable
private fun TinyState(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
) {
    val labelText: @Composable () -> Unit = {
        Text(text = label, style = LawTheme.typography.bodySecondary, color = tint)
    }

    Row(
        // A 14dp glyph is far under the 44dp minimum, so the whole chip is the target rather than
        // the icon. Read-only states simply have no target at all.
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(Metrics.CornerRadius))
                .clickable(onClickLabel = label, onClick = onClick)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
        } else {
            Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        labelText()
    }
}

private fun emptyTitle(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.All -> "Nothing collected yet"
    LibraryFilter.Saved -> "Nothing saved"
    LibraryFilter.Mastered -> "Nothing marked as read"
}

private fun emptyBody(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.All -> "Save a law and mark one as read, and both appear here."
    LibraryFilter.Saved -> "Tap the bookmark on a law to keep it in your strategies."
    LibraryFilter.Mastered -> "Open a law and tick it to record that you have read it."
}