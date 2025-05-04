package com.example.teleflow.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.teleflow.auth.AuthManager
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TeleFlowRepository
    private val authManager: AuthManager = AuthManager(application)
    
    // Legacy LiveData - for backward compatibility
    val allRecordings: LiveData<List<Recording>>
    
    // LiveData to observe current user's recordings
    private val _userRecordings = MediatorLiveData<List<Recording>>()
    val userRecordings: LiveData<List<Recording>> = _userRecordings
    
    // LiveData for user's recent recordings
    private val _userRecentRecordings = MediatorLiveData<List<Recording>>()
    val userRecentRecordings: LiveData<List<Recording>> = _userRecentRecordings
    
    // Source for the combined LiveData
    private var recordingsSource: LiveData<List<Recording>>? = null
    private var recentRecordingsSource: LiveData<List<Recording>>? = null
    
    init {
        val database = TeleFlowDatabase.getDatabase(application)
        repository = TeleFlowRepository.getInstance(
            database.scriptDao(),
            database.recordingDao(),
            database.userDao()
        )
        allRecordings = repository.allRecordings
        
        // Set up user recordings observers
        setupUserRecordingsObserver()
    }
    
    private fun setupUserRecordingsObserver() {
        // Observe user ID changes to update recording sources
        authManager.currentUserId.observeForever { userId ->
            // Remove previous sources
            if (recordingsSource != null) {
                _userRecordings.removeSource(recordingsSource!!)
            }
            if (recentRecordingsSource != null) {
                _userRecentRecordings.removeSource(recentRecordingsSource!!)
            }
            
            // If we have a logged-in user, get their recordings
            if (userId != null) {
                recordingsSource = repository.getUserRecordings(userId)
                recentRecordingsSource = repository.getRecentUserRecordings(userId, 5) // Limit to 5 recent recordings
                
                _userRecordings.addSource(recordingsSource!!) { recordings ->
                    _userRecordings.value = recordings
                }
                
                _userRecentRecordings.addSource(recentRecordingsSource!!) { recordings ->
                    _userRecentRecordings.value = recordings
                }
            } else {
                // No user logged in - empty lists
                _userRecordings.value = emptyList()
                _userRecentRecordings.value = emptyList()
            }
        }
    }
    
    fun getRecordingById(id: Int): LiveData<Recording> {
        return repository.getRecordingById(id)
    }
    
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>> {
        // Get the current user ID
        val userId = authManager.getCurrentUserId()
        
        // If we have a user ID, get recordings filtered by both script and user
        return if (userId != null) {
            repository.getRecordingsByScriptIdAndUserId(scriptId, userId)
        } else {
            repository.getRecordingsByScriptId(scriptId)
        }
    }
    
    fun insertRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        // Ensure the recording belongs to the current user
        val userId = authManager.getCurrentUserId() ?: 1 // Default to user 1 if not logged in
        val recordingWithUser = if (recording.userId == 0) recording.copy(userId = userId) else recording
        repository.insertRecording(recordingWithUser)
    }
    
    fun updateRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        // Ensure the recording belongs to the current user
        val userId = authManager.getCurrentUserId() ?: 1 // Default to user 1 if not logged in
        val recordingWithUser = if (recording.userId == 0) recording.copy(userId = userId) else recording
        repository.updateRecording(recordingWithUser)
    }
    
    fun deleteRecording(recording: Recording) = viewModelScope.launch(Dispatchers.IO) {
        // Only delete if it belongs to the current user
        val userId = authManager.getCurrentUserId() ?: return@launch
        if (recording.userId == userId) {
            repository.deleteRecording(recording)
        }
    }
    
    // Add a new recording based on a script ID and video URI
    fun addNewRecording(scriptId: Int, videoUri: String) = viewModelScope.launch(Dispatchers.IO) {
        // Get current timestamp
        val currentTime = System.currentTimeMillis()
        
        // Get the current user ID
        val userId = authManager.getCurrentUserId() ?: 1 // Default to user 1 if not logged in
        
        // Create and insert the recording
        val recording = Recording(
            id = 0, // Room will auto-generate the ID
            scriptId = scriptId,
            userId = userId,
            videoUri = videoUri,
            date = currentTime
        )
        repository.insertRecording(recording)
        
        // Update the script's lastUsed timestamp
        repository.updateScriptLastUsed(scriptId)
    }
} 