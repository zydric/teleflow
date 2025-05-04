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
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scriptId"), Index("userId")]
)
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val scriptId: Int,
    val userId: Int,
    val videoUri: String,
    val date: Long
) 