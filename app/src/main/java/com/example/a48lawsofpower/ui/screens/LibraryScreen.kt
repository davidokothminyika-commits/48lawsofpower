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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
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

/** The three states a law can be in *because the reader put it there*. */
enum class LibraryFilter(val label: String) {
    Saved("Saved"),
    Read("Read"),
    Annotated("Annotated"),
}

/**
 * Which laws fall under each filter.
 *
 * A function rather than three inline `filter` calls at the call site, because the counts and the
 * list have to come from the same answer - a pill that says "Annotated 4" beside a list showing two
 * annotated laws is worse than no count at all.
 *
 * [annotatedIds] is a set of law ids rather than a count, so "is this law annotated" is one lookup
 * instead of a scan per row.
 */
fun LibraryFilter.matches(law: LawOverview, annotatedIds: Set<Int>): Boolean = when (this) {
    LibraryFilter.Saved -> law.bookmarked
    LibraryFilter.Read -> law.read
    LibraryFilter.Annotated -> law.id in annotatedIds
}

/** Every law the reader has touched in any way. The headline figure for this screen. */
fun touchedLaws(laws: List<LawOverview>, annotatedIds: Set<Int>): Int =
    laws.count { it.bookmarked || it.read || it.id in annotatedIds }

/**
 * The Library: only what the reader has put here themselves.
 *
 * **There is deliberately no "All" filter.** This screen used to open on one, which rendered all 48
 * laws and made the Library a slower copy of the Dossier - the two tabs answered the same question
 * in different fonts, and neither of them answered the question this screen was named for. The full
 * corpus lives on the Dossier, with search and category filters; this is the shelf of what you
 * chose. Dropping "All" is what lets every remaining filter have something to show.
 *
 * Which makes the empty states load-bearing rather than decorative. A filter that can return
 * nothing has to say where the reader's work *is*, so [emptyBody] names the filters that do have
 * something in them rather than simply reporting the absence.
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

    val visible = laws.filter { filter.matches(it, annotatedLawIds) }
    val counts = LibraryFilter.entries.associateWith { entry ->
        laws.count { entry.matches(it, annotatedLawIds) }
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
            Spacer(Modifier.height(Spacing.xs))
            Text(
                // Says how much of the corpus this shelf covers, so the screen is legible as a
                // fraction of something rather than as an isolated list.
                text = "${touchedLaws(laws, annotatedLawIds)} of ${laws.size} laws kept, read or marked",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
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
                        count = counts[entry] ?: 0,
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
                body = emptyBody(filter, counts),
                modifier = Modifier.padding(top = Spacing.lg),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.sm,
                    // The header above is a sibling, not a list item, so the list's own padding has
                    // to clear the bottom bar the Scaffold installed.
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xl,
                ),
            ) {
                items(visible, key = { it.id }) { law ->
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
 *
 * The whole row is the tap target. It was title-only before, which meant the reader aimed at text
 * on a list they were scanning rather than swiping through it.
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
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
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
            // The saved state is the one the reader can change from here, so it gets a real
            // affordance. Read and annotated are consequences of reading, not switches.
            Spacer(Modifier.width(Spacing.sm))
            TinyState(
                icon = if (law.bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                label = if (law.bookmarked) "Saved" else "Save",
                tint = if (law.bookmarked) SovereignGold else ShadowGray,
                onClick = onToggleBookmark,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.padding(start = 28.dp + Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = law.category,
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
                modifier = Modifier.weight(1f),
            )
            if (law.read) {
                TinyState(Icons.Outlined.CheckCircleOutline, "Read", ObservanceGreen)
            }
            if (annotated) {
                TinyState(Icons.Outlined.EditNote, "Annotated", SovereignGold)
            }
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

/** Internal rather than private so the copy can be asserted without a device. */
internal fun emptyTitle(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.Saved -> "Nothing saved"
    LibraryFilter.Read -> "Nothing read yet"
    LibraryFilter.Annotated -> "Nothing marked"
}

/**
 * An empty state that points at the work that does exist.
 *
 * The reader arrives here from a filter they chose, so "nothing here" on its own is a dead end -
 * it reads as the app having lost their data. Naming the filters that *do* have something turns it
 * into the next useful tap.
 */
internal fun emptyBody(filter: LibraryFilter, counts: Map<LibraryFilter, Int>): String {
    val elsewhere = LibraryFilter.entries
        .filter { it != filter && (counts[it] ?: 0) > 0 }
        .joinToString(" and ") { "${it.label.lowercase()} (${counts[it]})" }

    val destination = when (filter) {
        LibraryFilter.Saved -> "Tap the bookmark on any law to keep it here."
        LibraryFilter.Read -> "Open a law and tick it to record that you have read it."
        LibraryFilter.Annotated -> "Select a passage in the reader to leave a highlight or a note."
    }

    return if (elsewhere.isEmpty()) destination else "$destination You have ${elsewhere}."
}