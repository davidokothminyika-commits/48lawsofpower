package com.example.a48lawsofpower.legal

/*
 * ============================================================================
 *  LEGAL TEXT - drafted in this repository for United States readers
 * ============================================================================
 *
 * These documents were drafted here, not by a lawyer, and they describe an offline app that
 * collects nothing. The decisions behind them are recorded so the next reader of this file
 * knows what was chosen and what is still owed:
 *
 *   - Jurisdiction: United States. That fixes the children's threshold at 13 (COPPA), names the
 *     Federal Trade Commission rather than a data-protection authority, and shapes the liability
 *     carve-out in section 7 of the Terms against state consumer-protection law.
 *   - Contact: the project's issue tracker, because no support mailbox is published.
 *   - Consent: agreement is a condition of use - the gate shows before any content does.
 *
 * Text inside [ ] is a note to the author: resolve it or delete it. None remain. If one comes
 * back, `isPlaceholder = true` must come back with it on that document, which restores the red
 * banner on the consent screen, the Legal tab and the document itself. The banner is the rail
 * that makes placeholder text impossible to ship unnoticed; it is not decoration.
 *
 * What has NOT happened yet: no lawyer has reviewed this text. Before release, have the
 * Terms of Use and the Privacy Policy read by someone qualified for the target market - section
 * 7 of the Terms (liability) and section 7 of the Privacy (state privacy-law rights) are the
 * two clauses where that matters most. If the review changes anything, raise the affected
 * document's `version` and `CONSENT_VERSION` in the same change: readers who accepted the old
 * text are re-prompted automatically - see `ConsentStore` for why that is version-gated.
 *
 * One exception to all of the above: `ATTRIBUTION` at the bottom of this file is real text and is
 * already marked `isPlaceholder = false`. It is a factual statement of where the law names come
 * from, not a set of terms, so there is nothing in it to review with a lawyer.
 *
 * Two factual points, recorded here because they are the ones this file is most likely to get
 * wrong:
 *
 *  - The AndroidManifest declares no `android.permission.INTERNET`. It is explicitly removed with
 *    `tools:node="remove"`, because Coil contributes it to the merged manifest. Nothing in the app
 *    opens a network connection, and section 5 of the Privacy Policy below says exactly that. If a
 *    future feature ever needs the network, the permission and that section have to change in the
 *    same commit.
 *  - Section 4 of the Privacy Policy describes `res/xml/data_extraction_rules.xml` and
 *    `res/xml/backup_rules.xml`. Those two files carry the real include rules; if they change,
 *    change the section with them.
 */

private const val PLACEHOLDER_NOTICE =
    "DRAFT - PLACEHOLDER TEXT. Not reviewed by a lawyer. Replace before release."

/**
 * The version every reader must have accepted to get past the consent gate.
 *
 * Stored alongside the acceptance record, so raising this value re-prompts everyone whose stored
 * version is lower. Raise it whenever the terms change in a way a reader would want to know about.
 */
const val CONSENT_VERSION = 3

private const val LAST_UPDATED = "20 October 2026"

/** The project's issue tracker. The only published route for notices, complaints and takedowns. */
private const val CONTACT_URL = "https://github.com/davidokothminyika-commits/48lawsofpower/issues"

private val TERMS = LegalDocument(
    kind = LegalDocumentKind.Terms,
    summary = "The rules for using the app: what it is, what it is not, and who it is for.",
    version = 2,
    lastUpdated = LAST_UPDATED,
    isPlaceholder = false,
    sections = listOf(
        LegalSection(
            heading = "1. Acceptance of these terms",
            paragraphs = listOf(
                "By tapping Accept you agree to these Terms of Use and to the Privacy Policy " +
                    "linked from this screen. If you do not agree, do not use the app.",
                "Agreement is a condition of use: the app shows its consent screen before it " +
                    "shows any content, and it does not run for a reader who has not accepted. " +
                    "There is no separate licence to read the content without accepting.",
            ),
        ),
        LegalSection(
            heading = "2. What the app is",
            paragraphs = listOf(
                "The app presents an annotated reading of the 48 Laws of Power: each law broken into a " +
                    "core judgement, the consequences of observing and of ignoring it, and the " +
                    "reversal of it. It also records which laws you have read and which you have " +
                    "saved to your own strategies.",
                "It is a reading and reference tool. It is not a course, a coach, or an adviser.",
            ),
        ),
        LegalSection(
            heading = "3. Educational content, not advice",
            paragraphs = listOf(
                "The 48 Laws are a historical study of power. The commentary on them describes how " +
                    "the dynamics work and how they are commonly observed or broken. Nothing in the " +
                    "app is a recommendation that you act on them.",
                "The app does not provide legal, financial, medical, or psychological advice. " +
                    "Where a law describes conduct that is unlawful, the app is describing the " +
                    "conduct, not endorsing it. Seek qualified advice on your own circumstances.",
            ),
        ),
        LegalSection(
            heading = "4. Your account and your data",
            paragraphs = listOf(
                "The app does not require an account. Your reading progress, your saved strategies " +
                    "and your annotations are stored on this device only. They are not linked to " +
                    "an identity and are not visible to anyone else.",
                "Uninstalling the app removes all of it from the device. What Android's system " +
                    "backup may carry to a new device or back after a reinstall is set out in " +
                    "section 4 of the Privacy Policy.",
            ),
        ),
        LegalSection(
            heading = "5. Your content and your conduct",
            bullets = listOf(
                "Do not use the app to break the law, or to plan conduct that harms anyone.",
                "Do not attempt to interfere with the app's operation or reverse engineer it " +
                    "except where that restriction is unenforceable by law.",
                "Do not redistribute the app or its text as your own.",
            ),
        ),
        LegalSection(
            heading = "6. Availability and changes",
            paragraphs = listOf(
                "The app is provided as is, without warranty. It may be changed, interrupted, or " +
                    "withdrawn at any time.",
                "These terms may be updated. Material changes are recorded by raising the version " +
                    "number below, which re-prompts every reader for consent before the app reopens.",
            ),
        ),
        LegalSection(
            heading = "7. Liability",
            paragraphs = listOf(
                "To the extent permitted by law, the authors are not liable for loss arising from " +
                    "use of, or reliance on, the content of this app.",
                "The app is offered as it stands. Where the law of your state implies warranties " +
                    "that cannot be waived, or does not permit a term to exclude a category of " +
                    "loss - personal injury, fraud, or whatever else that state treats as " +
                    "non-excludable - this section does not exclude it. Nothing here limits a " +
                    "liability that the law of your state refuses to limit.",
            ),
        ),
        LegalSection(
            heading = "8. Contact",
            paragraphs = listOf(
                "Notices, complaints, questions and takedown requests go to the project's issue " +
                    "tracker, which is read and where every notice reaches: $CONTACT_URL",
                "Issues are public. If your message needs a private reply, say so in the issue and " +
                    "give a route to reach you there - the app holds no address for you itself, " +
                    "because it holds nothing about you at all.",
            ),
        ),
    ),
)

private val PRIVACY = LegalDocument(
    kind = LegalDocumentKind.Privacy,
    summary = "What the app stores, where it stores it, and what it does not collect.",
    version = 2,
    lastUpdated = LAST_UPDATED,
    isPlaceholder = false,
    sections = listOf(
        LegalSection(
            heading = "1. Summary",
            paragraphs = listOf(
                "The app works entirely offline. It collects no personal data, has no accounts, " +
                    "runs no analytics or advertising SDKs, and makes no network requests.",
                "The only data it holds is what you create by using it: which laws you have read, " +
                    "which you have saved, and what you have marked or written in them. That data " +
                    "stays on this device.",
            ),
        ),
        LegalSection(
            heading = "2. What the app stores",
            bullets = listOf(
                "Reading progress - which of the 48 laws you have opened, and how far you are " +
                    "through one.",
                "Saved strategies - the laws you have bookmarked.",
                "Your annotations - highlights, notes and bookmarks you attach to the text.",
                "Your app preferences - theme, whether you have seen the introduction, and that " +
                    "you accepted these terms, with the version you accepted.",
            ),
        ),
        LegalSection(
            heading = "3. Where that data lives",
            paragraphs = listOf(
                "All of it is written to this app's private storage on this device. Android " +
                    "sandboxs that storage so other apps cannot read it, and it is removed when you " +
                    "uninstall the app.",
                "No copy is uploaded, synced, or shared. There is no server-side copy to delete, " +
                    "which also means there is no account to close.",
            ),
        ),
        LegalSection(
            heading = "4. Device backups",
            paragraphs = listOf(
                "Android's system backup can include this app's private storage, which means your " +
                    "reading progress may be carried to a new device or restored after a reinstall. " +
                    "It stays under your control through your device backup settings.",
                // Describe res/xml/data_extraction_rules.xml and res/xml/backup_rules.xml exactly.
                // Those two files hold the include rules; if they change, change this paragraph.
                "The app does not leave that to the platform default. Its backup rules name what may " +
                    "be carried: this app's database, which holds the law content together with your " +
                    "reading progress, saved strategies and annotations, and its preferences file, " +
                    "which holds your consent record and app preferences. Nothing else the app " +
                    "writes is included, and cache is never backed up.",
            ),
        ),
        LegalSection(
            heading = "5. Network access",
            paragraphs = listOf(
                "The app does not make network requests, and it does not hold the Android " +
                    "permission that would let it. Nothing in the app can open a connection, so " +
                    "there is no traffic for anyone to observe and no request to block.",
                // If a feature ever needs the network, add android.permission.INTERNET back in
                // AndroidManifest.xml and rewrite this paragraph in the same change.
            ),
        ),
        LegalSection(
            heading = "6. Children",
            paragraphs = listOf(
                "The app is not directed at children under 13 and it does not knowingly collect " +
                    "information from them. It never asks for a date of birth or any other detail " +
                    "that would tell one reader from another.",
                "Because it collects nothing from anyone - no account, no identifier, no analytics " +
                    "- it has nothing to collect from a child either. A parent who believes the " +
                    "app holds something about their child can use the contact route in section 8.",
            ),
        ),
        LegalSection(
            heading = "7. Your rights",
            paragraphs = listOf(
                "Because all data is local to this device, you exercise control directly: uninstall " +
                    "the app to erase everything, or revoke consent from the Legal tab to clear " +
                    "reading progress and saved strategies.",
                "If a state privacy law of yours grants rights over personal information - to know " +
                    "what is held about you, to delete it, to correct it, or to opt out of a sale " +
                    "or sharing that never happens here - the same two actions give you all of " +
                    "them. Section 2 is the complete list of what is held, it is all on this " +
                    "device, and nothing derived from it is sold, shared, or used to profile you. " +
                    "To ask what is held before you decide, use the contact route in section 8.",
            ),
        ),
        LegalSection(
            heading = "8. Contact and complaints",
            paragraphs = listOf(
                "Notices and requests go to the project's issue tracker, which is read: " +
                    "$CONTACT_URL Issues are public, so say there if you need a private reply and " +
                    "give a route to reach you.",
                "The app is built and published in the United States. If a complaint cannot be " +
                    "settled through the contact route above, a privacy complaint in the United " +
                    "States can be taken to the Federal Trade Commission at reportfraud.ftc.gov. " +
                    "There is no single data-protection authority for an app like this one: it is " +
                    "general-audience, keeps no identifier, and sells nothing.",
            ),
        ),
    ),
)

/**
 * Cookie notice.
 *
 * Deliberately short: the app makes no network requests, so there is nothing to disclose. Section 3
 * carries no drafting bracket for the same reason - the trigger it used to record is here instead:
 * if a future release ever loads remote content or embeds a WebView, this document has to be
 * rewritten to cover it, not merely re-read.
 */
private val COOKIES = LegalDocument(
    kind = LegalDocumentKind.Cookies,
    summary = "The short version: the app sets no cookies. This explains why, and what that means.",
    version = 2,
    lastUpdated = LAST_UPDATED,
    isPlaceholder = false,
    sections = listOf(
        LegalSection(
            heading = "1. The short version",
            paragraphs = listOf(
                "The app sets no cookies and runs no web view, no advertising, and no analytics. " +
                    "Nothing on this screen exists to change in a future release of a web service - " +
                    "the app never talks to one.",
                "This document is here because the notice has to exist and be reachable, not because " +
                    "there is anything to disclose.",
            ),
        ),
        LegalSection(
            heading = "2. Why a native app usually needs this notice and this one does not",
            paragraphs = listOf(
                "A cookie is a small file a website asks your browser to store so it can recognise " +
                    "you later. The concerns behind cookie notices - third parties tracking a reader " +
                    "across sites, profiling without consent, consent banners that are designed to be " +
                    "dismissed - all come from that mechanism.",
                "A native Android app that makes no network requests has no equivalent mechanism. " +
                    "Local storage written by the app is covered by the Privacy Policy instead, which " +
                    "is the correct instrument for it.",
            ),
        ),
        LegalSection(
            heading = "3. Local storage, for completeness",
            paragraphs = listOf(
                "The app stores your reading progress, your saved strategies, your annotations and " +
                    "your consent record in its own private storage. These are not cookies, they " +
                    "are not readable by other apps, and they never leave the device.",
            ),
        ),
        LegalSection(
            heading = "4. How to clear everything",
            paragraphs = listOf(
                "Revoke consent from the Legal tab, or uninstall the app. Both erase all local data " +
                    "permanently.",
            ),
        ),
    ),
)

/**
 * Attribution to the original author.
 *
 * Unlike the three documents above, this one never carried drafting notes: every statement in it
 * is a factual claim about where the names and ideas come from, so it has always been marked
 * `isPlaceholder = false` and needs no lawyer. It is deliberately accurate rather than reassuring
 * - an attribution notice records a fact, it does not by itself settle what that fact means
 * legally.
 */
private val ATTRIBUTION = LegalDocument(
    kind = LegalDocumentKind.Attribution,
    summary = "The 48 Laws of Power is the work of Robert Greene. This app is original " +
        "commentary on those laws, not the book.",
    version = 1,
    lastUpdated = LAST_UPDATED,
    isPlaceholder = false,
    sections = listOf(
        LegalSection(
            heading = "1. Who wrote what",
            paragraphs = listOf(
                "\"The 48 Laws of Power\" and the names of its 48 laws are the work of Robert Greene, " +
                    "first published in 1998.",
                "This app is not that book. It contains no text from it. Every summary, core " +
                    "reading, historical example, and reversal in the app is original commentary " +
                    "written for this app.",
                "The app exists because the laws are worth studying as a historical account of how " +
                    "power has been handled, not because the commentary could substitute for the " +
                    "book. If you want Greene's actual argument, his own cases, and his own prose, " +
                    "you have to read the book.",
            ),
        ),
        LegalSection(
            heading = "2. What this notice does and does not do",
            paragraphs = listOf(
                "This notice credits the author and states plainly what the app does and does not " +
                    "contain. That is all it does.",
                "It is not a permission, and displaying it does not make the app's use of the " +
                    "titles lawful. Whether a given use is permitted is decided by the copyright " +
                    "law of your jurisdiction and, in practice, by whoever owns the rights.",
                "If you are the rights holder and you want the use changed or stopped, the app's " +
                    "repository is linked from the store listing and every screen has a Legal tab. " +
                    "A takedown request will be honoured.",
            ),
            bullets = listOf(
                "Do not read this screen as legal advice or as a defence.",
                "Do assume it covers material added in a future version. If the content ever stops " +
                    "being original, this document has to change with it.",
            ),
        ),
        LegalSection(
            heading = "3. The code",
            paragraphs = listOf(
                "The application's source code is released under the MIT License. The licence " +
                    "covers the code only. It grants no rights in the book, in the titles of the " +
                    "laws, or in anyone else's material, and it does not license the MIT licence to " +
                    "override the copyright in those.",
            ),
        ),
    ),
)

/** Every document, in the order the consent screen and the Legal tab list them. */
val allLegalDocuments: List<LegalDocument> = listOf(TERMS, PRIVACY, COOKIES, ATTRIBUTION)

/** Resolves a document by kind. Returns `null` only for an unknown route, which the screen rejects. */
fun legalDocumentFor(kind: LegalDocumentKind): LegalDocument? =
    allLegalDocuments.firstOrNull { it.kind == kind }

/** True while any document still carries the drafting skeleton rather than reviewed text. */
val hasPlaceholderText: Boolean = allLegalDocuments.any { it.isPlaceholder }

/** The warning banner shown while [hasPlaceholderText] holds. */
val placeholderNotice: String = PLACEHOLDER_NOTICE