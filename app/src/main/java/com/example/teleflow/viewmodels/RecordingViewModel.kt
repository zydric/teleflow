package com.example.teleflow.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TeleFlowRepository
    val allRecordings: LiveData<List<Recording>>
    
    init {
        val database = TeleFlowDatabase.getDatabase(application)
        val scriptDao = database.scriptDao()
        val recordingDao = database.recordingDao()
        repository = TeleFlowRepository(scriptDao, recordingDao)
        allRecordings = repository.allRecordings
    }
    
    fun getRecordingById(id: Int): LiveData<Recording> {
        return repository.getRecordingById(id)
    }
    
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>> {
        return repository.getRecordingsByScriptId(scriptId)
    }
    
    fun insertRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertRecording(recording)
    }
    
    fun updateRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateRecording(recording)
    }
    
    fun deleteRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteRecording(recording)
    }
    
    // Add a new recording based on a script ID and video URI
    fun addNewRecording(scriptId: Int, videoUri: String) = viewModelScope.launch(Dispatchers.IO) {
        // Get current timestamp
        val currentTime = System.currentTimeMillis()
        
        // Create and insert the recording
        val recording = Recording(
            id = 0, // Room will auto-generate the ID
            scriptId = scriptId,
            videoUri = videoUri,
            date = currentTime
        )
        repository.insertRecording(recording)
        
        // Update the script's lastUsed timestamp
        repository.updateScriptLastUsed(scriptId)
    }
} 