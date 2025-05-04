package com.example.teleflow.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "scripts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int, // Foreign key to User
    val title: String,
    val content: String,
    val createdAt: Date = Date(),
    val lastModifiedAt: Date = Date(),
    val lastUsedAt: Date? = null // Timestamp when the script was last used for recording
) 