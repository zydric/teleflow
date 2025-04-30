package com.example.teleflow.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = Script::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scriptId")]
)
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val scriptId: Int,
    val videoUri: String,
    val date: Long
) 