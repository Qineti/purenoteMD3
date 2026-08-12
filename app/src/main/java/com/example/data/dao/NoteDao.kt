package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecycleBinNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteNote(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteNotes(ids: List<Long>, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreNotes(ids: List<Long>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentlyDeleteNote(id: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun permanentlyDeleteNotes(ids: List<Long>)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt <= :cutoffTime")
    suspend fun autoCleanRecycleBin(cutoffTime: Long)

    @Query("UPDATE notes SET folderId = :newFolderId WHERE id IN (:noteIds)")
    suspend fun updateNotesFolder(noteIds: List<Long>, newFolderId: Long?)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun removeFolderFromNotes(folderId: Long)
}
