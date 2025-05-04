package com.example.teleflow.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity class representing a user in the TeleFlow application.
 * This class stores user account information for local authentication.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val profileImagePath: String?,
    val createdAt: Date = Date(),
    val lastLoginAt: Date? = null
) 