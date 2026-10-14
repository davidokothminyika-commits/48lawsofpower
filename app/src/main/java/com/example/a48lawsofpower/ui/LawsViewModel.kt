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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Which of the four bottom tabs the reader is on. */
enum class RootDestination { Dossier, Strategies, Codex, Notes, Legal }

class LawsViewModel(private val lawDao: LawDao) : ViewModel() {

    private val allLaws: StateFlow<List<LawOverview>> = lawDao.observeAllLaws()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            _selectedLaw.value = lawDao.getLawByNumber(lawNumber)
            val law = _selectedLaw.value
            // Auto-mark as mastered once the reader actually reaches the law.
            if (law != null && !law.read) {
                lawDao.markLawAsRead(lawNumber, true)
            }
        }
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
