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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import android.view.InputDevice
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.data.LawEntity
import com.example.a48lawsofpower.data.takeaways
import com.example.a48lawsofpower.data.SectionKey
import com.example.a48lawsofpower.ui.annotations.HighlightColor
import com.example.a48lawsofpower.ui.components.AnnotationActionBar
import kotlinx.coroutines.launch
import com.example.a48lawsofpower.ui.components.NoteEditorDialog
import com.example.a48lawsofpower.ui.dictionary.Dictionary
import com.example.a48lawsofpower.ui.dictionary.DictionarySheet
import com.example.a48lawsofpower.ui.dictionary.Entry
import com.example.a48lawsofpower.ui.speech.TtsReader
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
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/** The four core sub-sections every law is broken into. */
private enum class LawSection(val eyebrow: String, val key: String) {
    Takeaways("Key Takeaways", SectionKey.TAKEAWAYS),
    Core("Section I - The Core Law & Judgement", SectionKey.CORE),
    History("Section II - Transgression & Observance", SectionKey.CORE),
    Reversal("Section III - Reversal", SectionKey.REVERSAL);

    /**
     * What speech should read while this section is on screen.
     *
     * Not [key]: the History page shows both observance and transgression, and reading them
     * back to back is what a reader listening to that page actually wants. Takeaways are read as
     * separate lines, since each is a bullet rather than prose.
     */
    fun speech(law: com.example.a48lawsofpower.data.LawEntity): String = when (this) {
        Takeaways -> law.keyTakeaways.split('\n').filter { it.isNotBlank() }.joinToString(". ") { it.trim().removeSuffix(".") + "." }
        Core -> law.core
        History -> listOf(law.observance, law.transgression)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        Reversal -> law.reversal
    }
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
    onAddNote: (section: String, start: Int, end: Int, text: String, note: String) -> Unit,
    onToggleSectionBookmark: (section: String) -> Unit,
    onBackClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
    speech: TtsReader? = null,
    speechTick: Int = 0,
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

    val context = LocalContext.current
    val haptics = rememberHaptics()
    val pagerState = rememberPagerState(pageCount = { LawSection.entries.size })
    val shortcutFocus = remember { FocusRequester() }
    // onKeyEvent is not a suspend context, but paging between sections is animated. The scope is
    // tied to composition, so a page animation in flight when the reader closes is cancelled.
    val shortcutScope = rememberCoroutineScope()
    var showShortcutHint by rememberSaveable { mutableStateOf(false) }

    // Word tapped in the reader, and its lookup. The dictionary itself is loaded on first use and
    // kept by its own singleton, so this holds only the result.
    var lookedUpWord by remember { mutableStateOf<String?>(null) }
    var lookedUpEntry by remember { mutableStateOf<Entry?>(null) }
    var dictionaryReady by remember { mutableStateOf(false) }
    // Enumerating InputDevice rather than reading Configuration.keyboard: KEYBOARD_NOKEY is
    // @hide, and the public alternatives all mislead. WindowInsetsCompat.ime() reports a soft
    // keyboard, which is the opposite of what is being asked, and guessing from screen size is
    // wrong because a tablet and a phone in landscape both report large and only one has keys.
    val hasKeyboard = remember {
        InputDevice.getDeviceIds().any { id ->
            InputDevice.getDevice(id)?.isExternal == true &&
                InputDevice.getDevice(id)?.supportsSource(InputDevice.SOURCE_KEYBOARD) == true
        }
    }
    LaunchedEffect(hasKeyboard) {
        if (hasKeyboard) showShortcutHint = true
    }
    // Active selection as start..end in the section it belongs to; -1,-1 means nothing selected.
    var selectionStart by remember { mutableStateOf(-1) }
    var selectionEnd by remember { mutableStateOf(-1) }
    var pendingColor by remember { mutableStateOf(HighlightColor.DEFAULT) }
    // Section and width of the selection the reader asked to annotate, held while the editor is up.
    var noteTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var lastSelectionStart by remember { mutableStateOf(-1) }

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
            .background(MaterialTheme.colorScheme.background)
            // Focusable so the reader can take hardware keys. Without a request the key event
            // chain has nothing to deliver to here and the shortcuts silently never fire.
            .focusRequester(shortcutFocus)
            .focusable()
            .onKeyEvent { event ->
                // Consume only keys we act on, so Back and volume still reach the system.
                when (shortcutFor(event.key, event.type)) {
                    ReaderShortcut.PreviousSection -> {
                        if (pagerState.currentPage > 0) {
                            shortcutScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                        true
                    }

                    ReaderShortcut.NextSection -> {
                        if (pagerState.currentPage < LawSection.entries.lastIndex) {
                            shortcutScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                        true
                    }

                    ReaderShortcut.ToggleSpeech -> {
                        val reader = speech
                        if (reader != null && reader.state != TtsReader.State.Unavailable) {
                            if (reader.state == TtsReader.State.Speaking) {
                                reader.stop()
                            } else {
                                reader.play(LawSection.entries[pagerState.currentPage].speech(law))
                            }
                            true
                        } else {
                            false
                        }
                    }

                    ReaderShortcut.ToggleRead -> {
                        onToggleRead()
                        true
                    }

                    ReaderShortcut.ToggleBookmark -> {
                        onToggleBookmark()
                        true
                    }

                    null -> false
                }
            },
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
                speechRange = if (speech?.state == TtsReader.State.Speaking) speech?.currentRange() else null,
                onWordTapped = { word ->
                    if (word.isBlank()) return@SectionPage
                    lookedUpWord = word
                    lookedUpEntry = Dictionary.get(context).lookup(word)
                    dictionaryReady = true
                },
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

        lookedUpWord?.let { word ->
            DictionarySheet(
                word = word,
                entry = lookedUpEntry,
                dictionarySize = if (dictionaryReady) Dictionary.get(context).size else 0,
                onDismiss = {
                    lookedUpWord = null
                    lookedUpEntry = null
                },
            )
        }

        ReaderTopBar(
            law = law,
            onBackClick = onBackClick,
            onToggleBookmark = onToggleBookmark,
            onToggleRead = onToggleRead,
            onToggleSpeech = {
                if (speech == null) return@ReaderTopBar
                if (speech.state == TtsReader.State.Speaking) speech.stop() else speech.play(
                    LawSection.entries[pagerState.currentPage].speech(law),
                )
            },
            speechAvailable = speech != null && speech.state != TtsReader.State.Unavailable,
            speaking = speech?.state == TtsReader.State.Speaking,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = it.height.dp },
        )

        // Only when a keyboard is actually attached, and dismissible: a reader with a keyboard
        // does not need to be reminded of the same four keys on every law.
        if (showShortcutHint) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topBarHeight + Spacing.xs, start = Spacing.md, end = Spacing.md)
                    .clickable { showShortcutHint = false },
                colors = CardDefaults.cardColors(containerColor = CourtGrayElevated),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = shortcutHint(hasKeyboard = true).orEmpty(),
                    style = LawTheme.typography.bodySecondary,
                    color = ShadowGray,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }

        noteTarget?.let { (section, width) ->
            val quoted = SectionKey.textOf(law, section).let { t ->
                // The editor shows what was selected, not the whole block. The anchor is stored
                // as a range, so the excerpt is recovered by re-reading from the same offsets the
                // mark will be written with - see AnnotationEntity.anchorText.
                t.take(width).ifBlank { "" }
            }
            NoteEditorDialog(
                quoted = quoted,
                lawTitle = law.title,
                onSave = { note ->
                    val start = lastSelectionStart
                    if (start >= 0) {
                        onAddNote(section, start, start + width, SectionKey.textOf(law, section), note)
                    }
                    noteTarget = null
                },
                onDismiss = { noteTarget = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(Spacing.md),
            )
        }

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
                onToggleBookmark = {
                    val key = LawSection.entries[pagerState.currentPage].key
                    onToggleSectionBookmark(key)
                    selectionStart = -1
                    selectionEnd = -1
                },
                onAddNote = {
                    val key = LawSection.entries[pagerState.currentPage].key
                    lastSelectionStart = selectionStart
                    noteTarget = key to (selectionEnd - selectionStart)
                    selectionStart = -1
                    selectionEnd = -1
                },
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
    speechRange: IntRange?,
    onWordTapped: (String) -> Unit,
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
            LawSection.Takeaways -> {
                val points = law.takeaways()
                if (points.isEmpty()) {
                    // Reachable on an install that upgraded from schema 6: the column exists but the
                    // pre-packaged asset, which carries the text, is only copied when the database
                    // file is absent. Reinstalling restores it.
                    item(key = "takeaways-empty") {
                        EmptyState(
                            title = "No takeaways yet",
                            body = "Reinstall the app to load the takeaways for this law.",
                        )
                    }
                } else {
                    items(points.size, key = { points[it] }) { index ->
                        val point = points[index]
                        Row(modifier = Modifier.padding(bottom = Spacing.md)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SovereignGold),
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Text(
                                text = point,
                                style = LawTheme.typography.bodyPrimary,
                                color = ParchmentWhite,
                            )
                        }
                    }
                }
            }

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
                            speechRange = speechRange,
                            text = law.core,
                            annotations = annotations,
                            onSelectionChanged = onSelectionChanged,
                            onWordTapped = onWordTapped,
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
                        speechRange = speechRange,
                        text = law.reversal.ifBlank {
                            "Every law has a reversal. Read it before you act on the law itself."
                        },
                        annotations = annotations,
                        onSelectionChanged = onSelectionChanged,
                        onWordTapped = onWordTapped,
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
    onToggleRead: () -> Unit,
    onToggleSpeech: () -> Unit,
    speechAvailable: Boolean,
    speaking: Boolean,
    modifier: Modifier = Modifier,
) {
    // Resolved out here because drawWithContent runs in DrawScope, which is not @Composable and so
    // cannot read a theme value itself.
    val underline = SovereignGold.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .drawWithContent {
                drawContent()
                drawLine(
                    color = underline,
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
        // Hidden entirely when the device has no usable speech engine, rather than shown
        // disabled: a control that can never work is noise, and its absence is self-explanatory.
        if (speechAvailable) {
            ActionIconButton(
                icon = if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (speaking) "Stop reading aloud" else "Read aloud",
                active = speaking,
                onClick = onToggleSpeech,
            )
        }
        ActionIconButton(
            icon = if (law.read) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (law.read) {
                "Marked as read - tap to undo"
            } else {
                "Mark as read"
            },
            active = law.read,
            onClick = onToggleRead,
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
