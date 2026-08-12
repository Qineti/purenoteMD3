package com.example.ui.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecycleBinUiState(
    val notes: List<Note> = emptyList(),
    val selectedNoteIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val snackbarMessage: String? = null
)

class RecycleBinViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecycleBinUiState> = combine(
        repository.recycleBinNotes,
        _selectedNoteIds,
        _isMultiSelectMode,
        _snackbarMessage
    ) { notes, selectedIds, isMultiSelect, snackbar ->
        RecycleBinUiState(
            notes = notes,
            selectedNoteIds = selectedIds,
            isMultiSelectMode = isMultiSelect,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecycleBinUiState()
    )

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            repository.restoreNote(noteId)
            showSnackbar("笔记已成功恢复")
        }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteNote(noteId)
            showSnackbar("已彻底删除笔记")
        }
    }

    fun toggleSelectNote(noteId: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
        _isMultiSelectMode.value = current.isNotEmpty()
    }

    fun selectAllNotes(noteIds: List<Long>) {
        _selectedNoteIds.value = noteIds.toSet()
        _isMultiSelectMode.value = true
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun batchRestoreSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restoreNotes(ids)
            clearSelection()
            showSnackbar("已成功恢复 ${ids.size} 条笔记")
        }
    }

    fun batchPermanentlyDeleteSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.permanentlyDeleteNotes(ids)
            clearSelection()
            showSnackbar("已彻底删除 ${ids.size} 条笔记")
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun snackbarShown() {
        _snackbarMessage.value = null
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecycleBinViewModel(repository) as T
        }
    }
}
