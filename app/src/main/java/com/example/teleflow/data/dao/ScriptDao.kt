package com.example.teleflow.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.teleflow.models.Script
import java.util.Date

@Dao
interface ScriptDao {
    // Get all scripts (should only be used for admin purposes)
    @Query("SELECT * FROM scripts ORDER BY title ASC")
    fun getAllScripts(): LiveData<List<Script>>
    
    // Get all scripts for a specific user
    @Query("SELECT * FROM scripts WHERE userId = :userId ORDER BY title ASC")
    fun getUserScripts(userId: Int): LiveData<List<Script>>
    
    // Get all scripts for a specific user (synchronous version)
    @Query("SELECT * FROM scripts WHERE userId = :userId ORDER BY title ASC")
    suspend fun getUserScriptsSync(userId: Int): List<Script>
    
    // Get recently used scripts for a specific user
    @Query("SELECT * FROM scripts WHERE userId = :userId AND lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT 3")
    fun getRecentlyUsedScripts(userId: Int): LiveData<List<Script>>
    
    // Legacy method - kept for backward compatibility
    @Query("SELECT * FROM scripts WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT 3")
    fun getRecentlyUsedScripts(): LiveData<List<Script>>
    
    // Get script by ID
    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getScriptById(id: Int): LiveData<Script>
    
    // Get script by ID (sync version)
    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptByIdSync(id: Int): Script?
    
    // Update last used timestamp
    @Query("UPDATE scripts SET lastUsedAt = :date, lastModifiedAt = :date WHERE id = :id")
    suspend fun updateLastUsed(id: Int, date: Date)
    
    // Insert a new script
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: Script): Long
    
    // Insert multiple scripts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scripts: List<Script>)
    
    // Update an existing script
    @Update
    suspend fun update(script: Script)
    
    // Delete a script
    @Delete
    suspend fun delete(script: Script)
    
    // Get script count for a specific user
    @Query("SELECT COUNT(*) FROM scripts WHERE userId = :userId")
    suspend fun getUserScriptCount(userId: Int): Int
    
    // Legacy method - kept for backward compatibility
    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun getScriptCount(): Int
} 