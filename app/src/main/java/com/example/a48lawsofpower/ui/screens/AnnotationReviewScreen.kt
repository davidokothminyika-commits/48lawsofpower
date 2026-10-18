package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.AnnotationFilter
import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.AnnotationWithLaw
import com.example.a48lawsofpower.ui.annotations.HighlightColor
import com.example.a48lawsofpower.ui.annotations.HighlightStrokes
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * Every mark the reader has made, in one list, filterable by type and colour.
 *
 * This exists because a highlight read in isolation is close to meaningless. Each row carries the
 * quoted passage, the law it came from, and - when there is one - the note, so the list answers
 * "why did I mark this" without opening anything. Tapping a row opens the law at that passage.
 *
 * Filters compose with the query rather than being separate queries, and the counts are not shown:
 * a count next to a filter that empties the list is a worse signal than the list itself.
 */
@Composable
fun AnnotationReviewScreen(
    annotations: List<AnnotationWithLaw>,
    filter: AnnotationFilter,
    canUndo: Boolean,
    onFilterChange: (AnnotationFilter) -> Unit,
    onOpenLaw: (lawNumber: Int) -> Unit,
    onEditNote: (AnnotationWithLaw) -> Unit,
    onDelete: (Long) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "title") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg, start = Spacing.md, end = Spacing.md, bottom = Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "My Notes",
                        style = LawTheme.typography.lawTitle,
                        color = ParchmentWhite,
                        fontFamily = FontFamily.Serif,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (canUndo) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CourtGrayElevated)
                                .clickable(onClick = onUndo),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Undo,
                                contentDescription = "Undo delete",
                                tint = SovereignGold,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (annotations.isEmpty()) "Nothing marked yet" else "${annotations.size} marks",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
            }
        }

        item(key = "filters") {
            FilterRow(filter = filter, onFilterChange = onFilterChange)
        }

        if (annotations.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = if (filter.isUnfiltered) "Nothing marked yet" else "Nothing matches",
                    body = if (filter.isUnfiltered) {
                        "Long-press any passage in the reader to highlight it."
                    } else {
                        "No marks of that kind. Clear the filter to see everything."
                    },
                )
            }
        }

        items(items = annotations, key = { it.id }) { row ->
            AnnotationRow(
                row = row,
                onOpen = { onOpenLaw(row.lawNumber) },
                onEditNote = { onEditNote(row) },
                onDelete = { onDelete(row.id) },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
            )
        }

        item(key = "tail") { Spacer(Modifier.height(Spacing.xl * 2)) }
    }
}

@Composable
private fun FilterRow(filter: AnnotationFilter, onFilterChange: (AnnotationFilter) -> Unit) {
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "type-mixed") {
                FilterPill(
                    label = "All",
                    selected = filter.type == AnnotationType.MIXED,
                    onClick = { onFilterChange(filter.copy(type = AnnotationType.MIXED)) },
                )
            }
            item(key = "type-highlight") {
                FilterPill(
                    label = "Highlights",
                    selected = filter.type == AnnotationType.Highlight.wire,
                    onClick = { onFilterChange(filter.copy(type = AnnotationType.Highlight.wire)) },
                )
            }
            item(key = "type-note") {
                FilterPill(
                    label = "Notes",
                    selected = filter.type == AnnotationType.Note.wire,
                    onClick = { onFilterChange(filter.copy(type = AnnotationType.Note.wire)) },
                )
            }
            item(key = "type-bookmark") {
                FilterPill(
                    label = "Bookmarks",
                    selected = filter.type == AnnotationType.Bookmark.wire,
                    onClick = { onFilterChange(filter.copy(type = AnnotationType.Bookmark.wire)) },
                )
            }
        }

        // Colour row.
        //
        // Composes with the type row rather than replacing it - filter.copy(type = ...) on each
        // pill leaves [AnnotationFilter.color] alone, so "green highlights" is reachable by
        // picking both, which is what a reader sorting their own marks actually wants.
        //
        // Notes and bookmarks are not colourable, so the row is hidden unless highlights are in
        // scope. Offering colour while "Notes" is selected would be a control that can only ever
        // return an empty list.
        val colourInScope = filter.type == AnnotationType.MIXED || filter.type == AnnotationType.Highlight.wire
        if (colourInScope) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "colour-any") {
                    FilterPill(
                        label = "Any colour",
                        selected = filter.color.isBlank(),
                        onClick = { onFilterChange(filter.copy(color = "")) },
                    )
                }
                items(
                    items = HighlightColor.entries.toList(),
                    key = { "colour-${it.id}" },
                ) { option ->
                    ColourFilterSwatch(
                        color = option,
                        selected = filter.color == option.id,
                        onClick = {
                            // Tapping the active colour clears it, so the row doubles as its own
                            // off switch and the reader never has to hunt for "Any colour".
                            onFilterChange(
                                filter.copy(
                                    color = if (filter.color == option.id) "" else option.id,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

/** One colour in the filter row. Shows the mark's own stroke, not the translucent fill. */
@Composable
private fun ColourFilterSwatch(
    color: HighlightColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val stroke = HighlightStrokes[color] ?: SovereignGold
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CourtGray)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ParchmentWhite else HairlineBorder,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(stroke),
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        color = if (selected) ParchmentWhite else ShadowGray,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) SovereignGold else CourtGray)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}

@Composable
private fun AnnotationRow(
    row: AnnotationWithLaw,
    onOpen: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = AnnotationType.fromWire(row.type) ?: AnnotationType.Highlight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CourtGray)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (type == AnnotationType.Bookmark) {
                Icon(
                    Icons.Outlined.Bookmark,
                    contentDescription = "Bookmark",
                    tint = SovereignGold,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (type == AnnotationType.Note) {
                                ShadowGray
                            } else {
                                HighlightStrokes[HighlightColor.fromId(row.color)] ?: SovereignGold
                            },
                        ),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Law ${row.lawNumber.toString().padStart(2, '0')} · ${row.lawTitle}",
                style = LawTheme.typography.eyebrow,
                color = SovereignGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (row.anchorText.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = row.anchorText,
                style = LawTheme.typography.bodySecondary,
                color = ParchmentWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (row.note.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = row.note,
                style = LawTheme.typography.bodySecondary,
                color = ObservanceGreen,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(CourtGrayElevated)
                    .padding(Spacing.sm),
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(Icons.Outlined.Edit, "Edit note", onEditNote)
            Spacer(Modifier.width(Spacing.md))
            RowIcon(Icons.Outlined.Delete, "Delete mark", onDelete)
        }
    }
}

@Composable
private fun RowIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = ShadowGray, modifier = Modifier.size(17.dp))
    }
}