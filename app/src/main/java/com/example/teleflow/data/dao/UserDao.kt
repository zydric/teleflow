package com.example.teleflow.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.teleflow.models.User
import java.util.Date
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): LiveData<User>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: Int): User?

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): LiveData<List<User>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long
    
    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): User?

    @Query("UPDATE users SET lastLoginAt = :date WHERE id = :userId")
    suspend fun updateLastLogin(userId: Int, date: Date)

    @Query("UPDATE users SET profileImagePath = :imagePath WHERE id = :userId")
    suspend fun updateProfileImage(userId: Int, imagePath: String)

    @Query("UPDATE users SET profileImagePath = NULL WHERE id = :userId")
    suspend fun clearProfileImage(userId: Int)
} 