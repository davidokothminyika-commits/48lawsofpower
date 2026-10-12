package com.example.a48lawsofpower.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.legal.CONSENT_VERSION
import com.example.a48lawsofpower.legal.ConsentRecord
import com.example.a48lawsofpower.legal.ConsentStatus
import com.example.a48lawsofpower.legal.ConsentStore
import com.example.a48lawsofpower.legal.consentStatusFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the reader's consent.
 *
 * Separate from [com.example.a48lawsofpower.ui.LawsViewModel] on purpose. [com.example.a48lawsofpower.ui.LawsViewModel]
 * opens the law database the moment it is constructed; consent has to be resolvable before anything
 * like that happens, so the two lifecycles must not be tied together.
 */
class LegalViewModel(private val store: ConsentStore) : ViewModel() {

    /**
     * Shared at `Eagerly` rather than `WhileSubscribed`. The gate is decided in `MainActivity` before
     * the app draws anything, so the read has to be running from construction - a lazily started
     * share would leave the status at [ConsentStatus.Unknown] and make every launch look like a
     * first launch.
     */
    private val stored: StateFlow<ConsentRecord> = store.record
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConsentRecord())

    /** What the consent gate should be doing right now. */
    val consent: StateFlow<ConsentStatus> = stored
        .map { consentStatusFor(it, CONSENT_VERSION) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConsentStatus.Unknown)

    /** The acceptance on record, for the Legal tab's status line. Shares [stored]; not a second read. */
    val record: StateFlow<ConsentRecord> = stored

    fun accept() {
        viewModelScope.launch {
            store.accept(version = CONSENT_VERSION, timestampMillis = System.currentTimeMillis())
        }
    }

    /**
     * Withdrawal. `MainActivity` observes [consent], so clearing the record tears the nav graph down
     * and puts the gate back on screen without any navigation bookkeeping - there is nothing left
     * to navigate back to once the graph is gone.
     */
    fun revoke() {
        viewModelScope.launch { store.revoke() }
    }
}