package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.legal.LegalDocumentKind
import com.example.a48lawsofpower.legal.legalDocumentFor
import com.example.a48lawsofpower.legal.placeholderNotice
import com.example.a48lawsofpower.ui.components.ActionIconButton
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.LegalClause
import com.example.a48lawsofpower.ui.components.PlaceholderBanner
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * Reads one legal document end to end.
 *
 * Used from two places - over the consent gate, and as a destination inside the nav graph - so it
 * takes its kind as a parameter and owns its own top bar rather than borrowing either host's.
 */
@Composable
fun LegalDocumentScreen(
    kind: LegalDocumentKind,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val document = legalDocumentFor(kind)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ActionIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
            )
            Text(
                text = document?.kind?.title?.uppercase() ?: "NOT FOUND",
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
            )
            Spacer(Modifier.height(Metrics.IconButtonSize))
        }
        Hairline(color = SovereignGold.copy(alpha = 0.35f))

        if (document == null) {
            // Only reachable from a hand-edited deep link; the in-app routes are typed.
            EmptyState(
                title = "Document not found",
                body = "This document is not part of this build.",
                modifier = Modifier.navigationBarsPadding(),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.lg,
                bottom = Spacing.xl,
            ),
        ) {
            item(key = "header") {
                Text(
                    text = document.kind.title,
                    style = LawTheme.typography.lawNumber.copy(fontSize = 30.sp, lineHeight = 36.sp),
                    color = ParchmentWhite,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Version ${document.version}  ·  ${document.lastUpdated}",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
                if (document.isPlaceholder) {
                    Spacer(Modifier.height(Spacing.lg))
                    PlaceholderBanner(notice = placeholderNotice)
                }
                Spacer(Modifier.height(Spacing.lg))
                Hairline(color = SovereignGold.copy(alpha = 0.3f))
            }

            document.sections.forEachIndexed { index, section ->
                item(key = "section-$index") {
                    LegalClause(
                        heading = section.heading,
                        paragraphs = section.paragraphs,
                        bullets = section.bullets,
                        first = index == 0,
                    )
                }
            }

            item(key = "footer") {
                Spacer(Modifier.height(Spacing.xl))
                Hairline()
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "End of document · Version ${document.version}",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
            }
        }
    }
}