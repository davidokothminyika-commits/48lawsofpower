package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.LawOverview
import com.example.a48lawsofpower.ui.LawsViewModel
import com.example.a48lawsofpower.ui.components.LawCardPreview
import com.example.a48lawsofpower.ui.components.TogglePill
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

private object DossierItemType {
    const val Header = "header"
    const val Filters = "filters"
    const val Law = "law"
    const val Filler = "filler"
}

/**
 * The Dossier - the curated directory of the 48 laws.
 */
@Composable
fun DossierScreen(
    laws: List<LawOverview>,
    categories: List<String>,
    selectedCategory: String?,
    totalLaws: Int,
    masteredLaws: Int,
    onCategorySelected: (String?) -> Unit,
    onLawClick: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptics = rememberHaptics()
    val activeFilter = selectedCategory ?: LawsViewModel.ALL_CATEGORIES

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "header", contentType = DossierItemType.Header) {
            RankHeader(totalLaws = totalLaws, masteredLaws = masteredLaws)
        }

        item(key = "filters", contentType = DossierItemType.Filters) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
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

        if (laws.isEmpty()) {
            item(key = "empty", contentType = DossierItemType.Filler) {
                EmptyState(
                    title = "Nothing filed here",
                    body = "No law in the dossier answers to that filter.",
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
                    // Long-press takes the law straight into My Strategies.
                    haptics.impactHeavy()
                    onToggleBookmark(law.lawNumber)
                },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }

        item(key = "tail", contentType = DossierItemType.Filler) {
            // Breathing room so the last card never collides with the bottom navigation.
            Spacer(Modifier.height(Spacing.xl * 2))
        }
    }
}

@Composable
private fun RankHeader(totalLaws: Int, masteredLaws: Int) {
    val rank = when (masteredLaws) {
        0 -> "Apprentice"
        in 1..10 -> "Courtier"
        in 11..24 -> "Strategist"
        in 25..47 -> "Power Player"
        else -> "Emperor"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xl, start = Spacing.md, end = Spacing.md, bottom = Spacing.sm),
    ) {
        Text(
            text = "THE DOSSIER",
            style = LawTheme.typography.eyebrow,
            color = ShadowGray,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = rank,
            style = LawTheme.typography.lawNumber.copy(fontSize = 34.sp, lineHeight = 40.sp),
            color = ParchmentWhite,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(if (masteredLaws > 0) ObservanceGreen else SovereignGold),
            )
            Text(
                text = "  $masteredLaws / $totalLaws LAWS MASTERED",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
        }
    }
}
