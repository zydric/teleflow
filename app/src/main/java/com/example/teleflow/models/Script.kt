package com.example.teleflow.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val lastUsed: Long = 0 // Timestamp when the script was last used for recording (in milliseconds)
) 