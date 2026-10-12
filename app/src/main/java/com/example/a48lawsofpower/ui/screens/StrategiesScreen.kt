package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.a48lawsofpower.data.LawOverview
import com.example.a48lawsofpower.ui.components.LawCardPreview
import com.example.a48lawsofpower.ui.components.MetadataRow
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * My Strategies - the laws the reader has taken up, bookmarked by tapping the flag or by
 * long-pressing a card in the Dossier.
 */
@Composable
fun StrategiesScreen(
    laws: List<LawOverview>,
    onLawClick: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            MetadataRow(
                text = "My Strategies",
                color = ShadowGray,
                modifier = Modifier.padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.xl,
                    bottom = Spacing.lg,
                ),
            )
        }

        if (laws.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "No strategies yet",
                    body = "Long-press a law in the Dossier, or tap its flag, to take it up here.",
                )
            }
        }

        items(items = laws, key = { it.id }) { law ->
            LawCardPreview(
                law = law,
                onClick = { onLawClick(law.lawNumber) },
                onToggleBookmark = { onToggleBookmark(law.lawNumber) },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }

        item(key = "tail") {
            Spacer(Modifier.height(Spacing.xl * 2))
        }
    }
}
