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
    // Legacy method - should only be used for admin purposes
    @Query("SELECT * FROM recordings ORDER BY date DESC")
    fun getAllRecordings(): LiveData<List<Recording>>
    
    // Get recordings for a specific user
    @Query("SELECT * FROM recordings WHERE userId = :userId ORDER BY date DESC")
    fun getUserRecordings(userId: Int): LiveData<List<Recording>>
    
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingById(id: Int): LiveData<Recording>
    
    // Get recordings for a specific script
    @Query("SELECT * FROM recordings WHERE scriptId = :scriptId ORDER BY date DESC")
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>>
    
    // Get recordings for a specific script and user
    @Query("SELECT * FROM recordings WHERE scriptId = :scriptId AND userId = :userId ORDER BY date DESC")
    fun getRecordingsByScriptIdAndUserId(scriptId: Int, userId: Int): LiveData<List<Recording>>
    
    // Get recent recordings for a specific user (limited to a certain count)
    @Query("SELECT * FROM recordings WHERE userId = :userId ORDER BY date DESC LIMIT :count")
    fun getRecentUserRecordings(userId: Int, count: Int): LiveData<List<Recording>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: Recording): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<Recording>)
    
    @Update
    suspend fun update(recording: Recording)
    
    @Delete
    suspend fun delete(recording: Recording)
} 