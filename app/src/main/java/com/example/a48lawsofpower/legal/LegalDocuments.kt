package com.example.a48lawsofpower.legal

/*
 * ============================================================================
 *  PLACEHOLDER LEGAL TEXT - NOT LEGAL ADVICE - MUST BE REPLACED BEFORE RELEASE
 * ============================================================================
 *
 * Everything below is a drafting skeleton written to show the app's structure. It has NOT been
 * reviewed by a lawyer and it does not describe a real commercial relationship, because I cannot
 * know one. Text inside [ ] is a note to the author: resolve it or delete it.
 *
 * Before shipping:
 *   1. Replace every clause with real terms drawn up for your jurisdiction.
 *   2. Set `isPlaceholder = false` on each document. That kills the red warning banner that is
 *      currently drawn at the top of every document and on the consent screen. It is deliberately
 *      impossible to ship this text without noticing.
 *   3. Bump `CONSENT_VERSION` and each document's `version`. Readers who accepted the old text are
 *      re-prompted automatically - see `ConsentStore` for why that is version-gated.
 *
 * One exception to all of the above: `ATTRIBUTION` at the bottom of this file is real text and is
 * already marked `isPlaceholder = false`. It is a factual statement of where the law names come
 * from, not a set of terms, so there is nothing in it to review with a lawyer.
 *
 * One factual note that is NOT placeholder: the AndroidManifest declares `android.permission.INTERNET`,
 * but no screen in this app currently opens a network connection. If that stays true, delete the
 * permission. Shipping an unused INTERNET permission invites scrutiny on store review and directly
 * contradicts a privacy policy that claims nothing leaves the device.
 */

private const val PLACEHOLDER_NOTICE =
    "DRAFT - PLACEHOLDER TEXT. Not reviewed by a lawyer. Replace before release."

/**
 * The version every reader must have accepted to get past the consent gate.
 *
 * Stored alongside the acceptance record, so raising this value re-prompts everyone whose stored
 * version is lower. Raise it whenever the terms change in a way a reader would want to know about.
 */
const val CONSENT_VERSION = 2

private val TERMS = LegalDocument(
    kind = LegalDocumentKind.Terms,
    summary = "The rules for using the app: what it is, what it is not, and who it is for.",
    version = 1,
    lastUpdated = "Not yet dated",
    isPlaceholder = true,
    sections = listOf(
        LegalSection(
            heading = "1. Acceptance of these terms",
            paragraphs = listOf(
                "By tapping Accept you agree to these Terms of Use and to the Privacy Policy " +
                    "linked from this screen. If you do not agree, do not use the app.",
                "[Confirm whether you want agreement to be a condition of use, or whether you would " +
                    "rather license the content to non-users who simply read it.]",
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
                "The app does not require an account. Your reading progress and saved strategies are " +
                    "stored on this device only. They are not linked to an identity and are not " +
                    "visible to anyone else.",
                "[State what happens on uninstall - see the Privacy Policy, clause on device storage.]",
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
                "[Have this clause reviewed against your consumer law. Several jurisdictions - the " +
                    "UK and the EU among them - do not permit certain exclusions for consumers, and " +
                    "a blanket warranty disclaimer may not hold.]",
            ),
        ),
        LegalSection(
            heading = "8. Contact",
            paragraphs = listOf(
                "[Add a real contact address here. Several regimes expect a named contact for " +
                    "complaints and notices. This is also the address Google's and Apple's review " +
                    "teams will look for.]",
            ),
        ),
    ),
)

private val PRIVACY = LegalDocument(
    kind = LegalDocumentKind.Privacy,
    summary = "What the app stores, where it stores it, and what it does not collect.",
    version = 1,
    lastUpdated = "Not yet dated",
    isPlaceholder = true,
    sections = listOf(
        LegalSection(
            heading = "1. Summary",
            paragraphs = listOf(
                "The app works entirely offline. It collects no personal data, has no accounts, " +
                    "runs no analytics or advertising SDKs, and makes no network requests.",
                "The only data it holds is what you create by using it: which laws you have read, " +
                    "and which you have saved. That data stays on this device.",
            ),
        ),
        LegalSection(
            heading = "2. What the app stores",
            bullets = listOf(
                "Reading progress - which of the 48 laws you have opened.",
                "Saved strategies - the laws you have bookmarked.",
                "Your consent record - that you accepted these terms, and the version you accepted.",
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
                "[Confirm what `res/xml/data_extraction_rules.xml` currently excludes. Both that " +
                    "file and `backup_rules.xml` still carry the stock Android Studio TODO comment - " +
                    "review and complete them before release.]",
            ),
        ),
        LegalSection(
            heading = "5. Network access",
            paragraphs = listOf(
                "The app does not make network requests. The AndroidManifest declares the " +
                    "INTERNET permission but no screen currently uses it.",
                "[Either delete the unused permission, or - if a future version adds a legitimate " +
                    "feature that needs it - document that feature here before shipping. A privacy " +
                    "policy and the manifest have to agree.]",
            ),
        ),
        LegalSection(
            heading = "6. Children",
            paragraphs = listOf(
                "The app is not directed at children under 13 and does not knowingly collect " +
                    "information from them. Because it collects nothing from anyone, it cannot " +
                    "collect from them either.",
                "[Confirm the age threshold for your primary market and state it explicitly.]",
            ),
        ),
        LegalSection(
            heading = "7. Your rights",
            paragraphs = listOf(
                "Because all data is local to this device, you exercise control directly: uninstall " +
                    "the app to erase everything, or revoke consent from the Legal tab to clear " +
                    "reading progress and saved strategies.",
                "[State the rights you extend to users in your jurisdiction - access, deletion, " +
                    "portability - and the mechanism, even where the answer is 'delete the app'.]",
            ),
        ),
        LegalSection(
            heading = "8. Contact and complaints",
            paragraphs = listOf(
                "[Add a real contact address and name the supervisory authority or regulator a user " +
                    "can complain to.]",
            ),
        ),
    ),
)

private val COOKIES = LegalDocument(
    kind = LegalDocumentKind.Cookies,
    summary = "The short version: the app sets no cookies. This explains why, and what that means.",
    version = 1,
    lastUpdated = "Not yet dated",
    isPlaceholder = true,
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
                "The app stores your reading progress, your saved strategies, and your consent record " +
                    "in its own private storage. These are not cookies, they are not readable by " +
                    "other apps, and they never leave the device.",
                "[If a future version ever loads remote content or embeds a WebView, this notice " +
                    "becomes real and has to be rewritten to cover it. Treat that as a trigger to " +
                    "revisit this document, not as a formality.]",
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
 * Unlike the three documents above this one is not a drafting skeleton and is not marked
 * [isPlaceholder]: every statement in it is a factual claim about where the names and ideas come
 * from, and it needs no lawyer. It is deliberately accurate rather than reassuring - an attribution
 * notice records a fact, it does not by itself settle what that fact means legally.
 */
private val ATTRIBUTION = LegalDocument(
    kind = LegalDocumentKind.Attribution,
    summary = "The 48 Laws of Power is the work of Robert Greene. This app is original " +
        "commentary on those laws, not the book.",
    version = 1,
    lastUpdated = "Not yet dated",
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