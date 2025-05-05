package com.example.teleflow.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.teleflow.models.User
import java.util.Date

/**
 * Data Access Object for User entity.
 * Provides methods to perform database operations related to users.
 */
@Dao
interface UserDao {
    /**
     * Get a user by email address
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
    
    /**
     * Get a user by ID as LiveData for reactive UI updates
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): LiveData<User>
    
    /**
     * Get a user by ID synchronously
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: Int): User?
    
    /**
     * Get all users
     */
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): LiveData<List<User>>
    
    /**
     * Get user count
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
    
    /**
     * Insert a new user
     * @return the new user's ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long
    
    /**
     * Update an existing user
     */
    @Update
    suspend fun update(user: User)
    
    /**
     * Authenticate a user with email and password hash
     * @return user if credentials are valid, null otherwise
     */
    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): User?
    
    /**
     * Update user's last login time
     */
    @Query("UPDATE users SET lastLoginAt = :date WHERE id = :userId")
    suspend fun updateLastLogin(userId: Int, date: Date)
    
    /**
     * Update user's profile image path
     */
    @Query("UPDATE users SET profileImagePath = :imagePath WHERE id = :userId")
    suspend fun updateProfileImage(userId: Int, imagePath: String)
    
    /**
     * Clear user's profile image path
     */
    @Query("UPDATE users SET profileImagePath = NULL WHERE id = :userId")
    suspend fun clearProfileImage(userId: Int)
} 