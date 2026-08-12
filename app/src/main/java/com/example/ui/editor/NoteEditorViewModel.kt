package com.example.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

data class ContentState(
    val title: String,
    val content: String
)

data class NoteEditorUiState(
    val noteId: Long = -1L,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val isLocked: Boolean = true, // 默认锁定（只读）
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSearchReplaceVisible: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val snackbarMessage: String? = null
)

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = initialNoteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<ContentState>()
    private val redoStack = mutableListOf<ContentState>()

    private var autoSaveJob: Job? = null
    private var historySaveJob: Job? = null
    private var isInitialLoaded = false

    init {
        loadNote()
    }

    private fun loadNote() {
        if (initialNoteId <= 0L) {
            // 新建笔记：默认进入编辑模式（解锁）
            _uiState.value = _uiState.value.copy(
                isLocked = false
            )
            isInitialLoaded = true
        } else {
            viewModelScope.launch {
                val note = repository.getNoteById(initialNoteId)
                if (note != null) {
                    val (words, chars) = FormatUtils.countWordsAndChars(note.content)
                    _uiState.value = _uiState.value.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        isPinned = note.isPinned,
                        folderId = note.folderId,
                        isLocked = true, // 默认锁定（只读）
                        wordCount = words,
                        charCount = chars
                    )
                }
                isInitialLoaded = true
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        if (_uiState.value.isLocked) return
        recordHistory()
        updateContentState(newTitle, _uiState.value.content)
        scheduleAutoSave()
    }

    fun onContentChange(newContent: String) {
        if (_uiState.value.isLocked) return
        recordHistory()
        updateContentState(_uiState.value.title, newContent)
        scheduleAutoSave()
    }

    private fun updateContentState(title: String, content: String) {
        val (words, chars) = FormatUtils.countWordsAndChars(content)
        _uiState.value = _uiState.value.copy(
            title = title,
            content = content,
            wordCount = words,
            charCount = chars
        )
    }

    private fun recordHistory() {
        historySaveJob?.cancel()
        historySaveJob = viewModelScope.launch {
            delay(300)
            val currentState = ContentState(_uiState.value.title, _uiState.value.content)
            if (undoStack.isEmpty() || undoStack.last() != currentState) {
                undoStack.add(currentState)
                if (undoStack.size > 50) undoStack.removeAt(0)
                redoStack.clear()
                updateUndoRedoStatus()
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = ContentState(_uiState.value.title, _uiState.value.content)
        redoStack.add(current)

        val previous = undoStack.removeAt(undoStack.lastIndex)
        updateContentState(previous.title, previous.content)
        updateUndoRedoStatus()
        scheduleAutoSave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = ContentState(_uiState.value.title, _uiState.value.content)
        undoStack.add(current)

        val next = redoStack.removeAt(redoStack.lastIndex)
        updateContentState(next.title, next.content)
        updateUndoRedoStatus()
        scheduleAutoSave()
    }

    private fun updateUndoRedoStatus() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun toggleLock() {
        val newLockState = !_uiState.value.isLocked
        _uiState.value = _uiState.value.copy(isLocked = newLockState)
        val msg = if (newLockState) "已锁定笔记（只读）" else "已解锁，可编辑"
        showSnackbar(msg)
    }

    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = newPinned)
        saveNoteNow()
        val msg = if (newPinned) "已置顶" else "已取消置顶"
        showSnackbar(msg)
    }

    fun toggleSearchReplace() {
        _uiState.value = _uiState.value.copy(
            isSearchReplaceVisible = !_uiState.value.isSearchReplaceVisible
        )
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun onReplaceQueryChanged(r: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = r)
    }

    fun replaceOne() {
        val state = _uiState.value
        if (state.searchQuery.isEmpty() || state.isLocked) return

        val content = state.content
        val idx = content.indexOf(state.searchQuery, ignoreCase = true)
        if (idx != -1) {
            recordHistory()
            val newContent = content.replaceRange(idx, idx + state.searchQuery.length, state.replaceQuery)
            updateContentState(state.title, newContent)
            scheduleAutoSave()
            showSnackbar("已替换 1 处匹配项")
        } else {
            showSnackbar("未找到匹配文本")
        }
    }

    fun replaceAll() {
        val state = _uiState.value
        if (state.searchQuery.isEmpty() || state.isLocked) return

        val content = state.content
        if (content.contains(state.searchQuery, ignoreCase = true)) {
            recordHistory()
            val newContent = content.replace(state.searchQuery, state.replaceQuery, ignoreCase = true)
            updateContentState(state.title, newContent)
            scheduleAutoSave()
            showSnackbar("已完成全部替换")
        } else {
            showSnackbar("未找到匹配文本")
        }
    }

    fun formatContent() {
        if (_uiState.value.isLocked) return
        val currentContent = _uiState.value.content
        val formatted = FormatUtils.formatContent(currentContent)
        if (formatted != currentContent) {
            recordHistory()
            updateContentState(_uiState.value.title, formatted)
            scheduleAutoSave()
            showSnackbar("排版格式化完成")
        } else {
            showSnackbar("内容无需额外格式化")
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // 500ms debounce
            saveNoteNow()
        }
    }

    fun saveNoteNow() {
        viewModelScope.launch {
            val state = _uiState.value
            val title = if (state.title.isBlank() && state.content.isNotBlank()) {
                state.content.take(15).replace("\n", " ")
            } else {
                state.title
            }

            if (title.isBlank() && state.content.isBlank()) {
                return@launch
            }

            val now = System.currentTimeMillis()
            if (state.noteId <= 0L) {
                val newNote = Note(
                    title = title,
                    content = state.content,
                    createdAt = now,
                    updatedAt = now,
                    isPinned = state.isPinned,
                    folderId = state.folderId
                )
                val id = repository.insertNote(newNote)
                _uiState.value = _uiState.value.copy(noteId = id)
            } else {
                val existing = repository.getNoteById(state.noteId)
                val updated = Note(
                    id = state.noteId,
                    title = title,
                    content = state.content,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    isPinned = state.isPinned,
                    folderId = state.folderId
                )
                repository.updateNote(updated)
            }
        }
    }

    fun exportToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val outputStream = context.contentResolver.openOutputStream(uri)
                val writer = OutputStreamWriter(outputStream)
                val exportText = "# ${state.title}\n\n${state.content}"
                writer.write(exportText)
                writer.flush()
                writer.close()
                showSnackbar("已成功导出到指定文件")
            } catch (e: Exception) {
                showSnackbar("导出失败：${e.localizedMessage}")
            }
        }
    }

    fun showSnackbar(msg: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = msg)
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(repository, noteId) as T
        }
    }
}
