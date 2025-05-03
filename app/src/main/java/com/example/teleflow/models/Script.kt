package com.example.teleflow.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Date = Date(),
    val lastModifiedAt: Date = Date(),
    val lastUsedAt: Date? = null // Timestamp when the script was last used for recording
) 