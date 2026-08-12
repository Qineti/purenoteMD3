package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分组（文件夹）实体类
 */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
