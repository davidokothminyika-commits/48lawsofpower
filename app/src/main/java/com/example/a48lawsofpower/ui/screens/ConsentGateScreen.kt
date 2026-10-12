package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.legal.LegalDocumentKind
import com.example.a48lawsofpower.legal.allLegalDocuments
import com.example.a48lawsofpower.legal.hasPlaceholderText
import com.example.a48lawsofpower.legal.placeholderNotice
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.LegalButton
import com.example.a48lawsofpower.ui.components.LegalButtonVariant
import com.example.a48lawsofpower.ui.components.LegalDocumentRow
import com.example.a48lawsofpower.ui.components.PlaceholderBanner
import com.example.a48lawsofpower.ui.components.icon
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObsidianBlack
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.TransgressionRed

/**
 * The first-launch gate.
 *
 * Rendered by `MainActivity` instead of by the nav graph, so nothing behind it exists yet: no
 * database is opened, no law is queried, and there is no back stack to pop into.
 *
 * The reader is not trapped either. Declining leaves them here with the option to read on and change
 * their mind, because a gate with no exit is the kind of thing a store review flags - and because a
 * reader who genuinely disagrees deserves to know what they declined rather than being dropped at a
 * home screen they cannot reach.
 */
@Composable
fun ConsentGateScreen(
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var agreed by remember { mutableStateOf(false) }
    var declined by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<LegalDocumentKind?>(null) }

    // Documents open over the gate rather than through the nav graph: the graph is not composed
    // until consent is granted, so there is no route to navigate to yet.
    reading?.let { kind ->
        LegalDocumentScreen(kind = kind, onBackClick = { reading = null })
        return
    }

    val haptics = rememberHaptics()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "BEFORE YOU ENTER",
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Terms & Privacy",
                style = LawTheme.typography.lawNumber.copy(fontSize = 32.sp, lineHeight = 38.sp),
                color = ParchmentWhite,
            )
            Spacer(Modifier.height(Spacing.md))
            Hairline(color = SovereignGold)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "This app stores everything on your device and sends nothing anywhere. " +
                    "Before you go in, here is what it does with what you do - and what it does not do.",
                style = LawTheme.typography.bodyPrimary,
                color = ParchmentWhite,
            )
            Spacer(Modifier.height(Spacing.lg))

            allLegalDocuments.forEach { document ->
                LegalDocumentRow(
                    title = document.kind.title,
                    summary = document.summary,
                    icon = document.kind.icon(),
                    onClick = { reading = document.kind },
                )
                Hairline()
            }

            if (hasPlaceholderText) {
                Spacer(Modifier.height(Spacing.lg))
                PlaceholderBanner(notice = placeholderNotice)
            }

            if (declined) {
                Spacer(Modifier.height(Spacing.lg))
                DeclinedNotice()
            }

            Spacer(Modifier.height(Spacing.xl))
            ConsentCheckbox(
                checked = agreed,
                onCheckedChange = {
                    haptics.selection()
                    agreed = it
                    if (it) declined = false
                },
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "You can read these documents at any time afterwards, and withdraw your " +
                    "agreement from the Legal tab.",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
            Spacer(Modifier.height(Spacing.xl))
        }

        // The decision, pinned below the scrolling copy so it stays reachable on a short screen
        // without the reader having to scroll back down to accept.
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
            LegalButton(
                text = "ACCEPT & ENTER",
                onClick = {
                    haptics.impactHeavy()
                    onAccept()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = agreed,
            )
            Spacer(Modifier.height(Spacing.sm))
            LegalButton(
                text = "DECLINE",
                onClick = {
                    haptics.impactHeavy()
                    declined = true
                    agreed = false
                },
                modifier = Modifier.fillMaxWidth(),
                variant = LegalButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun ConsentCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            // toggleable on the row, not the box: a 20dp checkbox is not a touch target, and
            // putting it on both would fire the toggle twice.
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = SovereignGold,
                uncheckedColor = ShadowGray,
                checkmarkColor = ObsidianBlack,
            ),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = "I have read and accept the Terms of Use and the Privacy Policy, and I " +
                "acknowledge the Cookie Notice.",
            style = LawTheme.typography.bodyPrimary,
            color = ParchmentWhite,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DeclinedNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(TransgressionRed.copy(alpha = 0.12f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "WITHOUT AGREEMENT",
            style = LawTheme.typography.eyebrow,
            color = TransgressionRed,
        )
        Text(
            text = "This app cannot be used without your agreement. Nothing has been recorded and " +
                "no law data has been opened. If you change your mind, tick the box above and enter.",
            style = LawTheme.typography.bodySecondary,
            color = ParchmentWhite,
        )
    }
}