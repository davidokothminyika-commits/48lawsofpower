package com.example.a48lawsofpower.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.data.LawEntity
import com.example.a48lawsofpower.data.SectionKey
import com.example.a48lawsofpower.ui.annotations.HighlightColor
import com.example.a48lawsofpower.ui.components.AnnotationActionBar
import com.example.a48lawsofpower.ui.components.AnnotatableText
import com.example.a48lawsofpower.ui.components.ActionIconButton
import com.example.a48lawsofpower.ui.components.AnalysisBlock
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.MetadataRow
import com.example.a48lawsofpower.ui.components.Stance
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Motion
import com.example.a48lawsofpower.ui.theme.motionDuration
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/** The three core sub-sections every law is broken into. */
private enum class LawSection(val eyebrow: String, val key: String) {
    Core("Section I - The Core Law & Judgement", SectionKey.CORE),
    History("Section II - Transgression & Observance", SectionKey.CORE),
    Reversal("Section III - Reversal", SectionKey.REVERSAL),
}

/**
 * Clearance for the floating top bar and the section indicator.
 *
 * The top bar floats over the pager, so the pages have to start below it. [ReaderTopInset] is only
 * a first-frame guess: the real height is measured in [LawDetailScreen] because it is the status
 * bar inset plus the button row, and the status bar is 24dp on some devices and 70dp on others.
 */
private val ReaderTopInset: Dp = 104.dp
private val ReaderBottomInset = 88.dp

/**
 * The law reader: a deep horizontal pager through the three sections of a single law.
 */
@Composable
fun LawDetailScreen(
    law: LawEntity?,
    annotations: List<AnnotationEntity>,
    onHighlight: (section: String, start: Int, end: Int, text: String, color: HighlightColor) -> Unit,
    onBackClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (law == null) {
        LoadingState(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        )
        return
    }

    val haptics = rememberHaptics()
    val pagerState = rememberPagerState(pageCount = { LawSection.entries.size })
    // Active selection as start..end in the section it belongs to; -1,-1 means nothing selected.
    var selectionStart by remember { mutableStateOf(-1) }
    var selectionEnd by remember { mutableStateOf(-1) }
    var pendingColor by remember { mutableStateOf(HighlightColor.DEFAULT) }

    // Measured, not assumed: the bar's height depends on the device's status bar inset, and a
    // hardcoded value left the section eyebrow printing underneath it on tall-status-bar devices.
    var topBarHeight by remember { mutableStateOf(ReaderTopInset) }

    // Settling onto a new section is a selection tick, per the haptic mapping.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { haptics.selection() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.md),
            pageSpacing = Spacing.md,
        ) { page ->
            // Ambient motion: content fades in from +10dp over 350ms as a section comes into view.
            val revealProgress = remember(page) { Animatable(0f) }
            // Read outside the effect: motionDuration reads composition state, and the effect
            // body is a suspend lambda, not a composable scope.
            val revealDuration = motionDuration(Motion.STANDARD)
            LaunchedEffect(page, revealDuration) {
                // Guidelines section 3: screen transitions 180-240ms. This was a hardcoded
                // 350ms, which is a `long` token and only appropriate for a first-run reveal.
                revealProgress.animateTo(
                    1f,
                    tween(durationMillis = revealDuration, easing = Motion.Standard),
                )
            }

            SectionPage(
                law = law,
                annotations = annotations,
                section = LawSection.entries[page],
                onSelectionChanged = { s, e ->
                    selectionStart = s
                    selectionEnd = e
                },
                reveal = revealProgress.value,
                topInset = topBarHeight + Spacing.sm,
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - revealProgress.value) * 10.dp.toPx()
                    alpha = revealProgress.value
                },
            )
        }

        ReaderTopBar(
            law = law,
            onBackClick = onBackClick,
            onToggleBookmark = onToggleBookmark,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = it.height.dp },
        )

        if (selectionEnd > selectionStart) {
            AnnotationActionBar(
                selectedColor = pendingColor,
                onColorChange = { pendingColor = it },
                onHighlight = {
                    val key = LawSection.entries[pagerState.currentPage].key
                    onHighlight(
                        key,
                        selectionStart,
                        selectionEnd,
                        SectionKey.textOf(law, key),
                        pendingColor,
                    )
                    selectionStart = -1
                    selectionEnd = -1
                },
                onAddNote = { /* the note editor lands with the review screen */ },
                onCancel = { selectionStart = -1; selectionEnd = -1 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
            )
        } else {
            SectionIndicator(
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun SectionPage(
    law: LawEntity,
    annotations: List<AnnotationEntity>,
    section: LawSection,
    onSelectionChanged: (Int, Int) -> Unit,
    reveal: Float,
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        // No horizontal padding here: the pager that hosts this page already insets every page by
        // Spacing.md. Repeating it doubled the reader's margins to 32dp against the 16dp used by
        // the list screens, so the same law read at a different width depending on the screen.
        contentPadding = PaddingValues(
            top = topInset,
            // Breathing room so the final paragraph clears the section indicator.
            bottom = ReaderBottomInset + Spacing.xl,
        ),
    ) {
        item(key = "eyebrow") {
            Column {
                MetadataRow(text = section.eyebrow)
                Spacer(Modifier.height(Spacing.md))
                Hairline(color = SovereignGold)
                Spacer(Modifier.height(Spacing.lg))
            }
        }

        when (section) {
            LawSection.Core -> {
                item(key = "core-title") {
                    Text(
                        text = law.title,
                        style = LawTheme.typography.lawTitle.copy(
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                        ),
                        color = ParchmentWhite,
                        modifier = Modifier.padding(bottom = Spacing.lg),
                    )
                }
                if (law.core.isNotBlank()) {
                    item(key = "core-judgement") {
                        AnnotatableText(
                            text = law.core,
                            annotations = annotations,
                            onSelectionChanged = onSelectionChanged,
                            style = LawTheme.typography.bodyPrimary,
                            color = ParchmentWhite,
                            modifier = Modifier.padding(bottom = Spacing.lg),
                        )
                    }
                }
                item(key = "core-quote") {
                    Hairline(color = ShadowGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        text = law.summary,
                        style = LawTheme.typography.lawTitle.copy(
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                        ),
                        color = ShadowGray,
                    )
                }
            }

            LawSection.History -> {
                if (law.observance.isNotBlank()) {
                    item(key = "history-observance") {
                        AnalysisBlock(
                            eyebrow = "Observance",
                            body = law.observance,
                            stance = Stance.Observance,
                            modifier = Modifier.padding(bottom = Spacing.lg),
                        )
                    }
                }
                if (law.transgression.isNotBlank()) {
                    item(key = "history-transgression") {
                        AnalysisBlock(
                            eyebrow = "Transgression",
                            body = law.transgression,
                            stance = Stance.Transgression,
                            modifier = Modifier.padding(bottom = Spacing.lg),
                        )
                    }
                }
            }

            LawSection.Reversal -> {
                item(key = "reversal") {
                    AnnotatableText(
                        text = law.reversal.ifBlank {
                            "Every law has a reversal. Read it before you act on the law itself."
                        },
                        annotations = annotations,
                        onSelectionChanged = onSelectionChanged,
                        style = LawTheme.typography.bodyPrimary,
                        color = ParchmentWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    law: LawEntity,
    onBackClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .drawWithContent {
                drawContent()
                drawLine(
                    color = SovereignGold.copy(alpha = 0.35f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
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
            text = "LAW ${law.lawNumber.toString().padStart(2, '0')}",
            style = LawTheme.typography.lawIndex,
            color = ShadowGray,
        )
        ActionIconButton(
            icon = if (law.bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (law.bookmarked) {
                "Remove from My Strategies"
            } else {
                "Add to My Strategies"
            },
            active = law.bookmarked,
            onClick = onToggleBookmark,
        )
    }
}

/** One hairline rule per section; the active one is Sovereign Gold. */
@Composable
private fun SectionIndicator(current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(LawSection.entries.size) { index ->
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(
                        if (index == current) SovereignGold else ShadowGray.copy(alpha = 0.4f),
                    ),
            )
        }
    }
}
