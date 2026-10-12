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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.legal.CONSENT_VERSION
import com.example.a48lawsofpower.legal.ConsentRecord
import com.example.a48lawsofpower.legal.LegalDocumentKind
import com.example.a48lawsofpower.legal.allLegalDocuments
import com.example.a48lawsofpower.legal.hasPlaceholderText
import com.example.a48lawsofpower.legal.placeholderNotice
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.LegalButton
import com.example.a48lawsofpower.ui.components.LegalButtonVariant
import com.example.a48lawsofpower.ui.components.LegalDocumentRow
import com.example.a48lawsofpower.ui.components.PlaceholderBanner
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.components.icon
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.TransgressionRed
import java.text.DateFormat
import java.util.Date

/**
 * The Legal tab - the standing home of the terms, the privacy policy and the cookie notice, plus
 * the record of what the reader agreed to.
 *
 * Reachable at any time from the bottom bar, which is the point: a notice nobody can find again
 * after the first launch is not really given.
 */
@Composable
fun LegalScreen(
    record: ConsentRecord,
    onOpenDocument: (LegalDocumentKind) -> Unit,
    onRevokeConsent: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var confirmingRevoke by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    if (confirmingRevoke) {
        RevokeDialog(
            onConfirm = {
                confirmingRevoke = false
                haptics.impactHeavy()
                onRevokeConsent()
            },
            onDismiss = { confirmingRevoke = false },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = Spacing.xl,
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.md,
                    ),
            ) {
                Text(
                    text = "LEGAL",
                    style = LawTheme.typography.eyebrow,
                    color = ShadowGray,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Terms & Privacy",
                    style = LawTheme.typography.lawNumber.copy(fontSize = 32.sp, lineHeight = 38.sp),
                    color = ParchmentWhite,
                )
                Spacer(Modifier.height(Spacing.md))
                Hairline(color = SovereignGold)
            }
        }

        if (hasPlaceholderText) {
            item(key = "placeholder") {
                PlaceholderBanner(
                    notice = placeholderNotice,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
        }

        item(key = "consent") {
            ConsentStatusCard(
                record = record,
                onRevokeClick = { confirmingRevoke = true },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }

        item(key = "documents-eyebrow") {
            Text(
                text = "DOCUMENTS",
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
                modifier = Modifier.padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.lg,
                    bottom = Spacing.sm,
                ),
            )
        }

        allLegalDocuments.forEach { document ->
            item(key = "document-${document.kind}") {
                Column {
                    LegalDocumentRow(
                        title = document.kind.title,
                        summary = document.summary,
                        icon = document.kind.icon(),
                        onClick = { onOpenDocument(document.kind) },
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                    Hairline(modifier = Modifier.padding(horizontal = Spacing.md))
                }
            }
        }

        item(key = "footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
            ) {
                Hairline()
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "48 Laws of Power  ·  Version ${appVersionName()}",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "All reading progress and saved strategies are stored on this device only.",
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                )
                Spacer(Modifier.height(Spacing.xl * 2))
            }
        }
    }
}

@Composable
private fun ConsentStatusCard(
    record: ConsentRecord,
    onRevokeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(CourtGray)
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ObservanceGreen),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "CONSENT GRANTED",
                style = LawTheme.typography.eyebrow,
                color = ObservanceGreen,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "You accepted version ${record.version} of these terms on " +
                "${formatDate(record.acceptedAtMillis)}.",
            style = LawTheme.typography.bodyPrimary,
            color = ParchmentWhite,
        )
        if (record.version != CONSENT_VERSION) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "The terms have since been revised to version $CONSENT_VERSION. " +
                    "You will be asked to review them at next launch.",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Hairline(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(Spacing.md))
        LegalButton(
            text = "WITHDRAW CONSENT",
            onClick = onRevokeClick,
            modifier = Modifier.fillMaxWidth(),
            variant = LegalButtonVariant.Destructive,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Withdraws your agreement, clears your reading progress and saved strategies, " +
                "and returns you to the consent gate.",
            style = LawTheme.typography.bodySecondary,
            color = ShadowGray,
        )
    }
}

@Composable
private fun RevokeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CourtGray,
        titleContentColor = ParchmentWhite,
        textContentColor = ShadowGray,
        title = {
            Text("Withdraw consent?", style = LawTheme.typography.lawTitle.copy(fontSize = 19.sp))
        },
        text = {
            Text(
                "This clears your reading progress and saved strategies, and returns you to the " +
                    "consent gate. It cannot be undone.",
                style = LawTheme.typography.bodySecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "WITHDRAW",
                    style = LawTheme.typography.actionLabel,
                    color = TransgressionRed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "KEEP",
                    style = LawTheme.typography.actionLabel,
                    color = ShadowGray,
                )
            }
        },
    )
}

/**
 * `java.text.DateFormat` rather than `java.time`: minSdk is 24 and `java.time` only ships from
 * API 26, with no core library desugaring configured in this module.
 */
private fun formatDate(millis: Long): String =
    if (millis <= 0L) "an earlier date" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    val info = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }
    return info?.versionName ?: "?"
}