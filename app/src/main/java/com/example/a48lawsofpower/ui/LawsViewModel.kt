package com.example.a48lawsofpower.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.LawDao
import com.example.a48lawsofpower.data.LawEntity
import com.example.a48lawsofpower.data.LawOverview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Which of the four bottom tabs the reader is on. */
enum class RootDestination { Dossier, Library, Codex, Profile }

/**
 * Loading state for a given number of loaded laws.
 *
 * Named and extracted so the direction of the comparison is unambiguous at the one call site that
 * matters. The screen branches `if (isLoading) skeletons else laws`, so an inverted predicate here
 * does not degrade gracefully - it shows the skeleton forever and the content never appears.
 */
internal fun isLoadingFor(lawCount: Int): Boolean = lawCount == 0

class LawsViewModel(private val lawDao: LawDao) : ViewModel() {

    private val allLaws: StateFlow<List<LawOverview>> = lawDao.observeAllLaws()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
 * True while the first read has not returned any laws.
 *
 * Exists because "no laws yet" and "no laws match" are different states and the list screen used to
 * render the second for both - so a cold start flashed "Nothing filed here" before the real content
 * landed. `allLaws` starts on an empty list because that is the only sensible default for a
 * StateFlow, and the pre-packaged asset always carries 48 laws, so "empty" means "not read yet".
 *
 * The predicate is [isLoadingFor] rather than an inline `isEmpty()` so the direction of the test is
 * visible at the point of use. The first version of this was `map { it.isNotEmpty() }`, which is the
 * exact inverse: the skeleton then rendered whenever the laws *had* arrived and the list never drew.
 */
val isLoading: StateFlow<Boolean> = allLaws
    .map { isLoadingFor(it.size) }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * The Dossier list, narrowed by the active filter pill and the search box. `null` means "all".
     *
     * Search and filter compose rather than replace one another: a reader who searches "enemy"
     * while a category pill is active expects the intersection, not one silently discarding the
     * other. An empty query is not a match-all special case in the query itself - it simply fails
     * to contain anything, so it needs no branch here.
     */
    val filteredLaws: StateFlow<List<LawOverview>> =
        combine(allLaws, _selectedCategory, _query) { laws, category, query ->
            val byCategory = if (category == null) laws else laws.filter { it.category == category }
            val needle = query.trim()
            if (needle.isEmpty()) {
                byCategory
            } else {
                byCategory.filter { law ->
                    law.title.contains(needle, ignoreCase = true) ||
                        law.summary.contains(needle, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    /** Every category present in the data, in a stable order, for the filter row. */
    val categories: StateFlow<List<String>> =
        combine(allLaws, _selectedCategory) { laws, category ->
            val names = laws.map { it.category }.distinct()
            // "All" first, then the reader's active filter parked at the end.
            buildList {
                add(ALL_CATEGORIES)
                addAll(names.filterNot { it == ALL_CATEGORIES || it == category })
                category?.takeIf { it != ALL_CATEGORIES }?.let(::add)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(ALL_CATEGORIES))

    /** My Strategies - the laws the reader has actively taken up. */
    val bookmarkedLaws: StateFlow<List<LawOverview>> = lawDao.observeBookmarkedLaws()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mastery progress, derived from the same query that feeds the list. */
    val lawCount: StateFlow<Int> =
        allLaws.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val masteredCount: StateFlow<Int> =
        allLaws.map { laws -> laws.count { it.read } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _selectedLaw = MutableStateFlow<LawEntity?>(null)
    val selectedLaw: StateFlow<LawEntity?> = _selectedLaw.asStateFlow()

    private val _dailyLaw = MutableStateFlow<LawEntity?>(null)
    val dailyLaw: StateFlow<LawEntity?> = _dailyLaw.asStateFlow()

    init {
        loadDailyLaw()
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category?.takeIf { it != ALL_CATEGORIES }
    }

    /**
     * Deterministic so the "Daily Codex" is the same law for the whole day and does not shuffle
     * on every configuration change or app restart.
     */
    private fun loadDailyLaw() {
        viewModelScope.launch {
            val dayIndex = ChronoUnit.DAYS.between(LocalDate.of(2024, 1, 1), LocalDate.now())
            val lawNumber = ((dayIndex % TOTAL_LAWS) + TOTAL_LAWS).mod(TOTAL_LAWS) + 1
            _dailyLaw.value = lawDao.getDailyLaw(lawNumber)
        }
    }

    fun loadLawDetail(lawNumber: Int) {
        // Idempotent: navigating back and forth to the same law must not re-run the query on
        // every recomposition of the detail destination.
        if (_selectedLaw.value?.lawNumber == lawNumber) return

        viewModelScope.launch {
            // Deliberately does NOT mark the law read. Opening a law used to be enough, which made
            // "mastered" mean "tapped once" and quietly inflated the streak; the reader now sets it
            // explicitly with [setRead], so the count reflects a decision rather than a tap.
            _selectedLaw.value = lawDao.getLawByNumber(lawNumber)
        }
    }

    /** Explicit mastery control. Deliberate, and reversible. */
    fun setRead(lawNumber: Int, read: Boolean) {
        // Optimistic: the button has to respond to the tap, and the row behind it is refreshed by
        // the `read` Flow anyway once the write lands.
        if (_selectedLaw.value?.lawNumber == lawNumber) {
            _selectedLaw.value = _selectedLaw.value?.copy(read = read)
        }
        viewModelScope.launch { lawDao.markLawAsRead(lawNumber, read) }
    }

    fun toggleBookmark(lawNumber: Int) {
        val current = _selectedLaw.value
            ?.takeIf { it.lawNumber == lawNumber }
            ?.bookmarked
            ?: allLaws.value.firstOrNull { it.lawNumber == lawNumber }?.bookmarked
            ?: return
        val next = !current

        viewModelScope.launch {
            lawDao.setBookmarked(lawNumber, next)
            if (_selectedLaw.value?.lawNumber == lawNumber) {
                _selectedLaw.value = _selectedLaw.value?.copy(bookmarked = next)
            }
        }
    }

    fun clearSelectedLaw() {
        _selectedLaw.value = null
    }

    companion object {
        const val ALL_CATEGORIES = "All"
        const val TOTAL_LAWS = 48
    }
}
