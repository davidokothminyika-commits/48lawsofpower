package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.LawEntity
import com.example.a48lawsofpower.ui.components.AnalysisBlock
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.MetadataRow
import com.example.a48lawsofpower.ui.components.Stance
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The Daily Codex - a single widget-like view cycling one power dynamic for the day. It rotates
 * deterministically from the date, so it does not reshuffle on every launch.
 */
@Composable
fun CodexScreen(
    law: LawEntity?,
    onLawClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    if (law == null) {
        LoadingState(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding),
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "codex-header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xl, start = Spacing.md, end = Spacing.md),
            ) {
                MetadataRow(text = "Daily Codex")
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                        .uppercase(),
                    style = LawTheme.typography.eyebrow,
                    color = ShadowGray,
                )
            }
        }

        item(key = "codex-number") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = law.lawNumber.toString().padStart(2, '0'),
                    style = LawTheme.typography.lawNumber.copy(
                        fontSize = 72.sp,
                        lineHeight = 78.sp,
                    ),
                    color = SovereignGold,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = law.title,
                    style = LawTheme.typography.lawTitle.copy(
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                    ),
                    color = ParchmentWhite,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                MetadataRow(text = law.category)
            }
        }

        item(key = "codex-rule") {
            Hairline(modifier = Modifier.padding(horizontal = Spacing.md))
        }

        item(key = "codex-core") {
            Column(Modifier.padding(Spacing.md)) {
                Text(
                    text = law.summary.ifBlank { law.core },
                    style = LawTheme.typography.bodyPrimary,
                    color = ParchmentWhite,
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "READ THE FULL DOSSIER ENTRY",
                    style = LawTheme.typography.eyebrow,
                    color = SovereignGold,
                    modifier = Modifier
                        .padding(top = Spacing.xs)
                        .clickable(onClick = { onLawClick(law.lawNumber) }),
                )
            }
        }

        if (law.observance.isNotBlank()) {
            item(key = "codex-observance") {
                AnalysisBlock(
                    eyebrow = "Observance",
                    body = law.observance,
                    stance = Stance.Observance,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.lg,
                    ),
                )
            }
        }

        if (law.transgression.isNotBlank()) {
            item(key = "codex-transgression") {
                AnalysisBlock(
                    eyebrow = "Transgression",
                    body = law.transgression,
                    stance = Stance.Transgression,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.lg,
                    ),
                )
            }
        }

        // Breathing room before the bottom navigation.
        item(key = "codex-tail") {
            Spacer(Modifier.height(Spacing.xl * 2))
        }
    }
}
