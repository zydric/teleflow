package com.example.teleflow.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.teleflow.models.Script

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY title ASC")
    fun getAllScripts(): LiveData<List<Script>>
    
    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getScriptById(id: Int): LiveData<Script>
    
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