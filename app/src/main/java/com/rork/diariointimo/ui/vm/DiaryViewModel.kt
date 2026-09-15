package com.rork.diariointimo.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.diariointimo.data.DiaryEntry
import com.rork.diariointimo.data.DiaryRepository
import com.rork.diariointimo.data.SignatureStroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class EntryFilter { ALL, FAVORITES }

data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val filter: EntryFilter = EntryFilter.ALL,
    val isLoaded: Boolean = false
)

/** Holds every diary page in memory while the diary is open and persists changes. */
class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DiaryRepository(application)

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    fun loadIfNeeded() {
        if (_uiState.value.isLoaded) return
        viewModelScope.launch {
            val stored = repository.load().sortedByDescending { it.updatedAt }
            _uiState.update { it.copy(entries = stored, isLoaded = true) }
        }
    }

    /** Clears in-memory content when the diary is locked again. */
    fun forget() {
        _uiState.value = DiaryUiState()
    }

    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun setFilter(value: EntryFilter) = _uiState.update { it.copy(filter = value) }

    fun entry(id: String?): DiaryEntry? =
        id?.let { key -> _uiState.value.entries.firstOrNull { it.id == key } }

    fun createEntry(date: LocalDate = LocalDate.now()): DiaryEntry {
        val now = System.currentTimeMillis()
        val entry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            epochDay = date.toEpochDay(),
            createdAt = now,
            updatedAt = now
        )
        _uiState.update { it.copy(entries = listOf(entry) + it.entries) }
        return entry
    }

    fun updateEntry(id: String, transform: (DiaryEntry) -> DiaryEntry) {
        _uiState.update { state ->
            state.copy(
                entries = state.entries.map { current ->
                    if (current.id == id) transform(current).copy(updatedAt = System.currentTimeMillis())
                    else current
                }
            )
        }
        persist()
    }

    fun toggleFavorite(id: String) = updateEntry(id) { it.copy(isFavorite = !it.isFavorite) }

    fun togglePrivate(id: String) = updateEntry(id) { it.copy(isPrivate = !it.isPrivate) }

    fun deleteEntry(id: String) {
        _uiState.update { state -> state.copy(entries = state.entries.filterNot { it.id == id }) }
        persist()
    }

    /** Replaces the sheet's finger-drawn signature. */
    fun setSignature(id: String, strokes: List<SignatureStroke>) =
        updateEntry(id) { it.copy(signature = strokes) }

    /** Drops a page that was opened but never written on. */
    fun discardIfBlank(id: String) {
        val entry = entry(id) ?: return
        if (entry.isBlank) deleteEntry(id)
    }

    private fun persist() {
        viewModelScope.launch { repository.save(_uiState.value.entries) }
    }
}

/** Pages that match the current search and filter, newest first. */
fun DiaryUiState.visibleEntries(): List<DiaryEntry> {
    val term = query.trim().lowercase()
    return entries
        .asSequence()
        .filter { if (filter == EntryFilter.FAVORITES) it.isFavorite else true }
        .filter { entry ->
            term.isEmpty() ||
                entry.title.lowercase().contains(term) ||
                entry.body.lowercase().contains(term)
        }
        .filterNot { it.isBlank }
        .sortedByDescending { it.epochDay * 100_000L + (it.updatedAt % 100_000L) }
        .toList()
}

fun DiaryUiState.entriesOn(date: LocalDate): List<DiaryEntry> =
    entries.filter { it.epochDay == date.toEpochDay() && !it.isBlank }
        .sortedByDescending { it.updatedAt }

fun DiaryUiState.daysWithEntries(): Set<Long> =
    entries.filterNot { it.isBlank }.map { it.epochDay }.toSet()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
