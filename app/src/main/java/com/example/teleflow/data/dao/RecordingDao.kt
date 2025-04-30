package com.example.teleflow.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.teleflow.models.Recording

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY date DESC")
    fun getAllRecordings(): LiveData<List<Recording>>
    
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingById(id: Int): LiveData<Recording>
    
    @Query("SELECT * FROM recordings WHERE scriptId = :scriptId ORDER BY date DESC")
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: Recording): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<Recording>)
    
    @Update
    suspend fun update(recording: Recording)
    
    @Delete
    suspend fun delete(recording: Recording)
} 