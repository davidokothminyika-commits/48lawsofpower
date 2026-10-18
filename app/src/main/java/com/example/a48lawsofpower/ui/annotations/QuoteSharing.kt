package com.example.a48lawsofpower.ui.annotations

import android.content.Context
import android.content.Intent

/**
 * Hands the reader's notes to another app.
 *
 * ## Why ACTION_SEND and not a file write
 *
 * The user asking "how do I get these out of this app" is almost always answered by the share
 * sheet, not by a files browser - they want it in a notes app, a mail, or a chat. Writing a file
 * and saying "go find it in Downloads" makes them do the last step themselves. Share offers every
 * installed app at once and is the one gesture everybody already knows.
 *
 * Markdown as `text/plain` rather than as a custom type: it renders as readable prose in every
 * target, so a target that does not understand Markdown still shows the text. A bespoke MIME type
 * would only be honoured by apps willing to implement it, of which there are none.
 *
 * The app holds no INTERNET permission, which does not affect this - the intent is resolved locally
 * and the receiving app does its own networking.
 */
fun shareExport(context: Context, markdown: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "48 Laws of Power — my notes")
        putExtra(Intent.EXTRA_TEXT, markdown)
    }
    // Not wrapped in a chooser at construction: the caller wraps it, so the app can start the
    // chooser as an activity and inherit the correct task from wherever the user is.
    context.startActivity(Intent.createChooser(send, "Share your notes").apply {
        // A chooser started from a non-activity context has no task to come back to.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}