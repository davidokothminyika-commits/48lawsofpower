package com.example.a48lawsofpower.legal

/**
 * The documents the app can present. [title] is the only user-facing string kept here; icons
 * and copy are chosen in the UI layer so this package stays free of Compose imports.
 */
enum class LegalDocumentKind(val title: String) {
    Terms("Terms of Use"),
    Privacy("Privacy Policy"),
    Cookies("Cookie Notice"),
    Attribution("Attribution & Copyright"),
}

/**
 * One numbered clause: a heading, the prose beneath it, and - where a clause is easier to scan as
 * a list than as a paragraph - a bullet run.
 */
data class LegalSection(
    val heading: String,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
)

/**
 * A complete legal document as the app ships it.
 *
 * [isPlaceholder] is a safety rail, not decoration. It renders a red banner at the top of the
 * document and drives the consent screen's warning. Set it to `false` per-document only once real,
 * reviewed text has replaced the drafting notes in `LegalDocuments.kt` - see the warning in that
 * file's header.
 */
data class LegalDocument(
    val kind: LegalDocumentKind,
    /** One or two sentences for the consent screen and the list row. */
    val summary: String,
    val sections: List<LegalSection>,
    /** Bumped whenever the text changes meaningfully; re-prompts every reader for consent. */
    val version: Int,
    val lastUpdated: String,
    val isPlaceholder: Boolean,
)