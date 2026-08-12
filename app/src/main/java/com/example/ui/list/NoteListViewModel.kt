package com.example.ui.list

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.SortOption
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class FolderFilter {
    object All : FolderFilter()
    object Uncategorized : FolderFilter()
    data class SelectedFolder(val folder: Folder) : FolderFilter()
}

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val currentFilter: FolderFilter = FolderFilter.All,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption(),
    val selectedNoteIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val snackbarMessage: String? = null
)

class NoteListViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _currentFilter = MutableStateFlow<FolderFilter>(FolderFilter.All)
    private val _sortOption = MutableStateFlow(SortOption())
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            // App启动时检查，自动清理超过30天的回收站笔记
            repository.autoCleanRecycleBin(30)
        }
    }

    val uiState: StateFlow<NoteListUiState> = combine(
        repository.allActiveNotes,
        repository.allFolders,
        _currentFilter,
        _searchQuery,
        _sortOption,
        _selectedNoteIds,
        _isMultiSelectMode,
        _snackbarMessage
    ) { notes, folders, filter, query, sort, selectedIds, isMultiSelect, snackbar ->

        // 1. 根据分组筛选
        var filtered = when (filter) {
            is FolderFilter.All -> notes
            is FolderFilter.Uncategorized -> notes.filter { it.folderId == null || it.folderId == 0L }
            is FolderFilter.SelectedFolder -> notes.filter { it.folderId == filter.folder.id }
        }

        // 2. 根据搜索关键词（标题和内容）筛选
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { note ->
                note.title.lowercase().contains(q) || note.content.lowercase().contains(q)
            }
        }

        // 3. 排序（置顶笔记排在列表最前面）
        val comparator = when (sort.field) {
            SortField.CREATED_AT -> compareBy<Note> { it.createdAt }
            SortField.UPDATED_AT -> compareBy<Note> { it.updatedAt }
            SortField.TITLE -> compareBy<Note> { it.title.lowercase() }
        }

        val sortedList = if (sort.direction == SortDirection.ASC) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(comparator.reversed())
        }

        // 置顶排前，其余按排序规则
        val finalNotes = sortedList.sortedByDescending { it.isPinned }

        NoteListUiState(
            notes = finalNotes,
            folders = folders,
            currentFilter = filter,
            searchQuery = query,
            sortOption = sort,
            selectedNoteIds = selectedIds,
            isMultiSelectMode = isMultiSelect,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFolderFilterSelected(filter: FolderFilter) {
        _currentFilter.value = filter
    }

    fun onSortOptionChanged(field: SortField, direction: SortDirection) {
        _sortOption.value = SortOption(field, direction)
    }

    fun togglePinStatus(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            repository.updateNote(updated)
            showSnackbar(if (updated.isPinned) "已置顶笔记" else "已取消置顶")
        }
    }

    fun softDeleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.softDeleteNote(noteId)
            showSnackbar("笔记已移入回收站")
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

    fun batchSoftDeleteSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.softDeleteNotes(ids)
            clearSelection()
            showSnackbar("已将 ${ids.size} 条笔记移入回收站")
        }
    }

    fun batchMoveSelectedToFolder(folderId: Long?) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveNotesToFolder(ids, folderId)
            clearSelection()
            showSnackbar("已移动 ${ids.size} 条笔记")
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertFolder(name.trim())
            showSnackbar("已创建分组：${name.trim()}")
        }
    }

    fun updateFolder(folder: Folder, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateFolder(folder.copy(name = newName.trim()))
            showSnackbar("已重命名分组")
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder.id)
            if (_currentFilter.value is FolderFilter.SelectedFolder &&
                (_currentFilter.value as FolderFilter.SelectedFolder).folder.id == folder.id) {
                _currentFilter.value = FolderFilter.All
            }
            showSnackbar("已删除分组：${folder.name}")
        }
    }

    fun importNoteFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()

                // 获取文件名作为默认标题
                var title = "导入的笔记"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        val fileName = cursor.getString(nameIndex)
                        if (!fileName.isNull_or_blank()) {
                            title = fileName.substringBeforeLast(".")
                        }
                    }
                }

                val currentFolderId = (_currentFilter.value as? FolderFilter.SelectedFolder)?.folder?.id

                val newNote = Note(
                    title = title,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    folderId = currentFolderId
                )

                repository.insertNote(newNote)
                showSnackbar("成功导入笔记：$title")
            } catch (e: Exception) {
                showSnackbar("导入失败：${e.localizedMessage}")
            }
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
            return NoteListViewModel(repository) as T
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
