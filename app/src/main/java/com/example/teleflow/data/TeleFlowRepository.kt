package com.example.teleflow.data

import androidx.lifecycle.LiveData
import com.example.teleflow.data.dao.RecordingDao
import com.example.teleflow.data.dao.ScriptDao
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script

class TeleFlowRepository(
    private val scriptDao: ScriptDao,
    private val recordingDao: RecordingDao
) {
    // Scripts
    val allScripts: LiveData<List<Script>> = scriptDao.getAllScripts()
    
    suspend fun insertScript(script: Script): Long {
        return scriptDao.insert(script)
    }
    
    suspend fun insertAllScripts(scripts: List<Script>) {
        scriptDao.insertAll(scripts)
    }
    
    fun getScriptById(id: Int): LiveData<Script> {
        return scriptDao.getScriptById(id)
    }
    
    suspend fun updateScript(script: Script) {
        scriptDao.update(script)
    }
    
    suspend fun deleteScript(script: Script) {
        scriptDao.delete(script)
    }
    
    // Recordings
    val allRecordings: LiveData<List<Recording>> = recordingDao.getAllRecordings()
    
    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insert(recording)
    }
    
    suspend fun insertAllRecordings(recordings: List<Recording>) {
        recordingDao.insertAll(recordings)
    }
    
    fun getRecordingById(id: Int): LiveData<Recording> {
        return recordingDao.getRecordingById(id)
    }
    
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>> {
        return recordingDao.getRecordingsByScriptId(scriptId)
    }
    
    suspend fun updateRecording(recording: Recording) {
        recordingDao.update(recording)
    }
    
    suspend fun deleteRecording(recording: Recording) {
        recordingDao.delete(recording)
    }
} 