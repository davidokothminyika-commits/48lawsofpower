package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.legal.LegalDocumentKind
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObsidianBlack
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.TransgressionRed

/**
 * The red warning drawn over any document still carrying the drafting skeleton.
 *
 * It exists so placeholder legal text cannot be shipped by accident: the text looks like a finished
 * document in a screenshot, and the only reliable way to catch that in review is a banner that is
 * impossible to miss. It disappears on its own once `isPlaceholder` is false on every document.
 */
@Composable
fun PlaceholderBanner(notice: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(TransgressionRed.copy(alpha = 0.12f))
            .border(
                width = Metrics.HairlineWidth,
                color = TransgressionRed.copy(alpha = 0.55f),
                shape = RoundedCornerShape(Metrics.CornerRadius),
            )
            .padding(Spacing.md),
    ) {
        Text(
            text = "NOTICE",
            style = LawTheme.typography.eyebrow,
            color = TransgressionRed,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = notice,
            style = LawTheme.typography.bodySecondary,
            color = ParchmentWhite,
        )
    }
}

enum class LegalButtonVariant { Primary, Secondary, Destructive }

/**
 * Flat, squared-off button matching the editorial chrome - Material's default pill and its ripple
 * do not belong in this app.
 */
@Composable
fun LegalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LegalButtonVariant = LegalButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val (background, foreground, border) = when (variant) {
        LegalButtonVariant.Primary -> Triple(SovereignGold, ObsidianBlack, SovereignGold)
        LegalButtonVariant.Secondary -> Triple(Color.Transparent, ParchmentWhite, HairlineBorder)
        LegalButtonVariant.Destructive -> Triple(Color.Transparent, TransgressionRed, TransgressionRed)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(Metrics.CornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground,
            // Material's default is a translucent scrim, which reads as "disabled but lit" against
            // this flat palette. A flat drop to 30% alpha says the same thing without glowing.
            disabledContainerColor = background.copy(alpha = 0.3f),
            disabledContentColor = foreground.copy(alpha = 0.4f),
        ),
        border = BorderStroke(Metrics.HairlineWidth, border),
    ) {
        Text(text = text, style = LawTheme.typography.actionLabel)
    }
}

/** The document's glyph. Kept in the UI layer so the legal package has no Compose dependency. */
fun LegalDocumentKind.icon(): ImageVector = when (this) {
    LegalDocumentKind.Terms -> Icons.Outlined.Description
    LegalDocumentKind.Privacy -> Icons.Outlined.Security
    LegalDocumentKind.Cookies -> Icons.Outlined.Cookie
    LegalDocumentKind.Attribution -> Icons.Outlined.Balance
}

/**
 * One tappable document in a list: glyph, title, one-line summary, chevron.
 *
 * The whole row is the target rather than the chevron, which by itself would be well under the
 * 44dp minimum.
 */
@Composable
fun LegalDocumentRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Description,
    trailingText: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Metrics.IconButtonSize)
                .clip(RoundedCornerShape(Metrics.CornerRadius))
                .background(SovereignGold.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SovereignGold,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LawTheme.typography.lawTitle.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = ParchmentWhite,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = summary,
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = LawTheme.typography.eyebrow,
                color = ShadowGray,
            )
            Spacer(Modifier.width(Spacing.xs))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ShadowGray,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Rebuilds a run of legal prose with its URLs made tappable.
 *
 * The contact clauses publish a bare `https://` address, and a bare address inside a Compose
 * `Text` is not a link. `Text` opens a [LinkAnnotation.Url] through the platform `UriHandler`,
 * which hands it to the browser, so the app itself still opens no connection of its own and
 * needs no permission for it.
 */
private val HttpsPattern = Regex("https://\\S+")

/**
 * @param linkColor the accent to draw addresses in. Taken as a parameter rather than read from the
 * theme inside, because the theme's colours are `@Composable` getters and this is a plain function.
 */
private fun linkified(text: String, linkColor: Color): AnnotatedString {
    val styles = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )

    return buildAnnotatedString {
        var cursor = 0
        HttpsPattern.findAll(text).forEach { match ->
            // A URL at the end of a sentence swallows the full stop; the punctuation belongs to the
            // sentence, not to the address.
            val raw = match.value
            val end = raw.indexOfLast { it !in ".,;:!?)]\\]}" }.let { if (it < 0) 0 else it + 1 }
            val url = raw.substring(0, end)
            val trailing = raw.substring(end)

            if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
            withLink(LinkAnnotation.Url(url = url, styles = styles)) { append(url) }
            append(trailing)
            cursor = match.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

/** Heading + optional body copy, used down the length of every legal document. */
@Composable
fun LegalClause(
    heading: String,
    paragraphs: List<String>,
    bullets: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    first: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!first) Spacer(Modifier.height(Spacing.xl))
        Text(
            text = heading,
            style = LawTheme.typography.lawTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
            color = ParchmentWhite,
        )
        paragraphs.forEach { paragraph ->
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = linkified(paragraph, linkColor = SovereignGold),
                style = LawTheme.typography.bodyPrimary,
                color = ParchmentWhite,
            )
        }
        if (bullets.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                bullets.forEach { bullet ->
                    Row {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SovereignGold.copy(alpha = 0.7f)),
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            text = bullet,
                            style = LawTheme.typography.bodyPrimary,
                            color = ParchmentWhite,
                        )
                    }
                }
            }
        }
    }
}