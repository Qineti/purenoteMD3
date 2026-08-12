package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体类
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folderId: Long? = null, // null 表示 "未分组"
    val isDeleted: Boolean = false, // 软删除，标记在回收站
    val deletedAt: Long? = null
)
