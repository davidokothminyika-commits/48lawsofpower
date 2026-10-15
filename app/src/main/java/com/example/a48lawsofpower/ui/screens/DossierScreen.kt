package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.LawOverview
import com.example.a48lawsofpower.ui.LawsViewModel
import com.example.a48lawsofpower.ui.components.LawCardPreview
import com.example.a48lawsofpower.ui.components.SkeletonLawCard
import com.example.a48lawsofpower.ui.components.TogglePill
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/** Roughly one screen of cards. More than this is never visible at once. */
private const val SKELETON_ROWS = 6

private object DossierItemType {
    const val Header = "header"
    const val Search = "search"
    const val Hero = "hero"
    const val Section = "section"
    const val Filters = "filters"
    const val Law = "law"
    const val Filler = "filler"
}

/**
 * Home - the reader's entry point, laid out to the reference design: a compact rank line, a
 * search field, a featured card for one law, then the full directory.
 *
 * The previous layout put the rank header at display size and went straight into the list. It was
 * dense in the wrong way - ten cards on screen with no hierarchy, so nothing said "start here".
 * The featured card carries that job instead, and the header shrinks to two lines.
 */
@Composable
fun DossierScreen(
    laws: List<LawOverview>,
    isLoading: Boolean,
    featured: LawOverview?,
    categories: List<String>,
    selectedCategory: String?,
    query: String,
    totalLaws: Int,
    masteredLaws: Int,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onLawClick: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptics = rememberHaptics()
    val activeFilter = selectedCategory ?: LawsViewModel.ALL_CATEGORIES
    val searching = query.isNotBlank()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "header", contentType = DossierItemType.Header) {
            RankHeader(totalLaws = totalLaws, masteredLaws = masteredLaws)
        }

        item(key = "search", contentType = DossierItemType.Search) {
            LawSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }

        // Suppressed while searching: a "read now" prompt is noise when the reader already said
        // what they are looking for.
        if (!searching) {
            featured?.let { spot ->
                item(key = "hero", contentType = DossierItemType.Hero) {
                    FeaturedLawCard(
                        law = spot,
                        onClick = { onLawClick(spot.lawNumber) },
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
            }
        }

        item(key = "section", contentType = DossierItemType.Section) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (searching) "RESULTS" else "ALL LAWS",
                    style = LawTheme.typography.eyebrow,
                    color = ParchmentWhite,
                )
                Text(
                    text = "${laws.size} of $totalLaws",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
            }
        }

        item(key = "filters", contentType = DossierItemType.Filters) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(categories, key = { it }) { category ->
                    TogglePill(
                        label = category,
                        selected = category == activeFilter,
                        onClick = {
                            haptics.selection()
                            onCategorySelected(category.takeIf { it != LawsViewModel.ALL_CATEGORIES })
                        },
                    )
                }
            }
        }

        // Exactly one of: placeholders, the empty state, or the laws. These were once siblings,
        // which meant a loading list drew skeletons *and* real cards at once - visible as a screen
        // with six grey rows stacked above the actual content.
        if (isLoading) {
            items(SKELETON_ROWS, key = { "skeleton-$it" }) {
                SkeletonLawCard()
            }
        } else {
            if (laws.isEmpty()) {
                item(key = "empty", contentType = DossierItemType.Filler) {
                    EmptyState(
                        title = if (searching) "No match" else "Nothing filed here",
                        body = if (searching) {
                            "No law answers to that search."
                        } else {
                            "No law in the dossier answers to that filter."
                        },
                    )
                }
            }

            items(
                items = laws,
                key = { it.id },
                contentType = { DossierItemType.Law },
            ) { law ->
                LawCardPreview(
                    law = law,
                    onClick = { onLawClick(law.lawNumber) },
                    onToggleBookmark = { onToggleBookmark(law.lawNumber) },
                    onLongClick = {
                        haptics.impactHeavy()
                        onToggleBookmark(law.lawNumber)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 7.dp),
                )
            }
        }

        item(key = "tail", contentType = DossierItemType.Filler) {
            Spacer(Modifier.height(Spacing.xl * 2))
        }
    }
}

/** Search across law titles and summaries. See `LawsViewModel.filteredLaws` for the matching rule. */
@Composable
private fun LawSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text("Search laws...", style = LawTheme.typography.bodySecondary, color = ShadowGray)
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = ShadowGray)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = ShadowGray,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CourtGray,
            unfocusedContainerColor = CourtGray,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedTextColor = ParchmentWhite,
            unfocusedTextColor = ParchmentWhite,
            cursorColor = SovereignGold,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The featured law.
 *
 * Carries the crimson accent and a gold rule so it reads as the entry point rather than just
 * another row. Tapping opens the reader, which is what "Read now" promises.
 */
@Composable
private fun FeaturedLawCard(
    law: LawOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CourtGrayElevated)
            .clickable(onClick = onClick),
    ) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "LAW ${law.lawNumber.toString().padStart(2, '0')}",
            style = LawTheme.typography.eyebrow,
            color = SovereignGold,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = law.title,
            style = LawTheme.typography.lawTitle,
            color = ParchmentWhite,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Read Now →",
            style = LawTheme.typography.bodyPrimary,
            color = SovereignGold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.md))
        // A crimson foot rule, so the card is identifiable at a glance while scrolling.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(SovereignGold),
        )
    }
}

/**
 * The app title, with reading progress underneath.
 *
 * The rank ladder that used to sit here ("Apprentice", "Courtier", "Strategist", "Power Player",
 * "Emperor") is gone. It was a title for progress, but it competed with the app's own name for
 * the same slot, and a reader who has read two laws was told they were an "Apprentice" - a grade
 * rather than a fact. The number below says the same thing without pretending to be a judgement.
 */
@Composable
private fun RankHeader(totalLaws: Int, masteredLaws: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = Spacing.lg,
                start = Spacing.md,
                end = Spacing.md,
                bottom = Spacing.sm,
            ),
    ) {
        // "48" carries the crimson so the title matches the splash and the featured card.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "48",
                style = LawTheme.typography.lawTitle,
                color = SovereignGold,
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Laws of Power",
                style = LawTheme.typography.lawTitle,
                color = ParchmentWhite,
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (masteredLaws > 0) ObservanceGreen else SovereignGold),
            )
            Text(
                text = "  $masteredLaws of $totalLaws laws read",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
                fontSize = 13.sp,
            )
        }
    }
}