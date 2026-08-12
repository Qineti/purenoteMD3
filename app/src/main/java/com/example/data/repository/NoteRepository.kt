package com.example.data.repository

import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.model.Folder
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    val allActiveNotes: Flow<List<Note>> = noteDao.getAllActiveNotesFlow()
    val recycleBinNotes: Flow<List<Note>> = noteDao.getRecycleBinNotesFlow()
    val allFolders: Flow<List<Folder>> = folderDao.getAllFoldersFlow()

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun softDeleteNote(id: Long) = noteDao.softDeleteNote(id)
    suspend fun softDeleteNotes(ids: List<Long>) = noteDao.softDeleteNotes(ids)

    suspend fun restoreNote(id: Long) = noteDao.restoreNote(id)
    suspend fun restoreNotes(ids: List<Long>) = noteDao.restoreNotes(ids)

    suspend fun permanentlyDeleteNote(id: Long) = noteDao.permanentlyDeleteNote(id)
    suspend fun permanentlyDeleteNotes(ids: List<Long>) = noteDao.permanentlyDeleteNotes(ids)

    suspend fun autoCleanRecycleBin(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        noteDao.autoCleanRecycleBin(cutoff)
    }

    suspend fun moveNotesToFolder(noteIds: List<Long>, folderId: Long?) {
        noteDao.updateNotesFolder(noteIds, folderId)
    }

    suspend fun insertFolder(name: String): Long {
        return folderDao.insertFolder(Folder(name = name))
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folderId: Long) {
        noteDao.removeFolderFromNotes(folderId)
        folderDao.deleteFolderById(folderId)
    }
}
