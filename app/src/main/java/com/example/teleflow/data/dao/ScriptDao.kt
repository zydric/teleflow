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
    @Query("SELECT * FROM scripts ORDER BY title ASC")
    fun getAllScripts(): LiveData<List<Script>>
    
    @Query("SELECT * FROM scripts WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT 3")
    fun getRecentlyUsedScripts(): LiveData<List<Script>>
    
    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getScriptById(id: Int): LiveData<Script>
    
    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptByIdSync(id: Int): Script?
    
    @Query("UPDATE scripts SET lastUsedAt = :date, lastModifiedAt = :date WHERE id = :id")
    suspend fun updateLastUsed(id: Int, date: Date)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: Script): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scripts: List<Script>)
    
    @Update
    suspend fun update(script: Script)
    
    @Delete
    suspend fun delete(script: Script)
    
    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun getScriptCount(): Int
} 