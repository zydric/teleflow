package com.example.teleflow.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.teleflow.auth.AuthManager
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class ScriptViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TeleFlowRepository
    private val authManager: AuthManager = AuthManager(application)
    private val TAG = "ScriptViewModel"
    
    // LiveData to observe scripts for the current user
    private val _userScripts = MediatorLiveData<List<Script>>()
    val userScripts: LiveData<List<Script>> = _userScripts
    
    // LiveData to observe recently used scripts for the current user
    private val _userRecentScripts = MediatorLiveData<List<Script>>()
    val userRecentScripts: LiveData<List<Script>> = _userRecentScripts
    
    // Keep backward compatibility for now
    val allScripts: LiveData<List<Script>>
    val recentlyUsedScripts: LiveData<List<Script>>
    
    // Sources for the combined LiveData
    private var scriptsSource: LiveData<List<Script>>? = null
    private var recentScriptsSource: LiveData<List<Script>>? = null
    
    init {
        val database = TeleFlowDatabase.getDatabase(application)
        repository = TeleFlowRepository.getInstance(
            database.scriptDao(),
            database.recordingDao(),
            database.userDao()
        )
        
        // For backward compatibility
        allScripts = repository.allScripts
        recentlyUsedScripts = repository.recentlyUsedScripts
        
        // Update scripts when user changes
        setupUserScriptsObserver()
    }
    
    private fun setupUserScriptsObserver() {
        // Observe user ID changes to update script sources
        authManager.currentUserId.observeForever { userId ->
            // Remove previous sources
            if (scriptsSource != null) {
                _userScripts.removeSource(scriptsSource!!)
            }
            if (recentScriptsSource != null) {
                _userRecentScripts.removeSource(recentScriptsSource!!)
            }
            
            // If we have a logged-in user, get their scripts
            if (userId != null) {
                Log.d(TAG, "Setting up script observers for user: $userId")
                scriptsSource = repository.getUserScripts(userId)
                recentScriptsSource = repository.getUserRecentlyUsedScripts(userId)
                
                _userScripts.addSource(scriptsSource!!) { scripts ->
                    Log.d(TAG, "Received ${scripts.size} scripts from repository for user: $userId")
                    _userScripts.value = scripts
                }
                
                _userRecentScripts.addSource(recentScriptsSource!!) { scripts ->
                    _userRecentScripts.value = scripts
                }
            } else {
                // No user logged in - empty lists
                Log.d(TAG, "No user logged in, using empty lists")
                _userScripts.value = emptyList()
                _userRecentScripts.value = emptyList()
            }
        }
    }
    
    // Manually refresh scripts for a specific user
    fun refreshUserScripts(userId: Int) = viewModelScope.launch {
        try {
            Log.d(TAG, "Manually refreshing scripts for user: $userId")
            
            // Get updated scripts from repository on IO thread
            val updatedScripts = withContext(Dispatchers.IO) {
                // Force Room to refresh by making a new query
                repository.getUserScriptsSync(userId)
            }
            
            // Update LiveData on main thread
            withContext(Dispatchers.Main) {
                // Remove previous source if it exists
                if (scriptsSource != null) {
                    _userScripts.removeSource(scriptsSource!!)
                }
                
                // Get fresh LiveData from repository
                scriptsSource = repository.getUserScripts(userId)
                
                // Add the new source on main thread
                _userScripts.addSource(scriptsSource!!) { scripts ->
                    Log.d(TAG, "Refresh returned ${scripts.size} scripts")
                    _userScripts.value = scripts
                }
                
                // Additionally, we can also update the value immediately with the data we already have
                _userScripts.value = updatedScripts
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing scripts", e)
        }
    }
    
    fun getScriptById(id: Int): LiveData<Script> {
        return repository.getScriptById(id)
    }
    
    fun insertScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Set current user ID if not already set
            val userId = authManager.getCurrentUserId() ?: 1 // Default to user 1 if not logged in
            val scriptWithUser = if (script.userId == 0) script.copy(userId = userId) else script
            Log.d(TAG, "Inserting script: ${scriptWithUser.title} with userId: ${scriptWithUser.userId}")
            val scriptId = repository.insertScript(scriptWithUser)
            Log.d(TAG, "Script inserted with ID: $scriptId")
            
            // Switch to main thread to update LiveData
            withContext(Dispatchers.Main) {
                refreshUserScripts(scriptWithUser.userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting script", e)
        }
    }
    
    fun updateScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Ensure the script belongs to the current user
            val userId = authManager.getCurrentUserId() ?: 1 // Default to user 1 if not logged in
            val scriptWithUser = if (script.userId == 0) script.copy(userId = userId) else script
            Log.d(TAG, "Updating script: ${scriptWithUser.title} with userId: ${scriptWithUser.userId}")
            repository.updateScript(scriptWithUser)
            
            // Switch to main thread to update LiveData
            withContext(Dispatchers.Main) {
                refreshUserScripts(scriptWithUser.userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating script", e)
        }
    }
    
    fun deleteScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Only delete if it belongs to the current user
            val userId = authManager.getCurrentUserId() ?: return@launch
            if (script.userId == userId) {
                Log.d(TAG, "Deleting script: ${script.title} with userId: ${script.userId}")
                repository.deleteScript(script)
                
                // Switch to main thread to update LiveData
                withContext(Dispatchers.Main) {
                    refreshUserScripts(userId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting script", e)
        }
    }
    
    fun updateScriptLastUsed(scriptId: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.updateScriptLastUsed(scriptId)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating script last used", e)
        }
    }
    
    // Function to initialize the database with a welcome script for a specific user
    fun insertSampleScripts(userId: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Check if the user already has scripts (avoid duplicates)
            val scriptCount = repository.getUserScriptCount(userId)
            if (scriptCount > 0) {
                // User already has scripts, don't create a welcome script
                return@launch
            }
            
            val now = Date()
            val welcomeScript = Script(
                id = 0,
                userId = userId,
                title = "Welcome to TeleFlow",
                content = "Thank you for choosing TeleFlow as your teleprompter app! This is a sample script to help you get started.\n\n" +
                        "With TeleFlow, you can:\n" +
                        "• Create and edit scripts for your recordings\n" +
                        "• Record videos while reading your script\n" +
                        "• Organize your scripts and recordings\n\n" +
                        "To get started, you can edit this script or create a new one from the scripts tab. " +
                        "When you're ready to record, simply open a script and tap the record button.\n\n" +
                        "We hope you enjoy using TeleFlow!",
                createdAt = now,
                lastModifiedAt = now,
                lastUsedAt = now
            )
            repository.insertScript(welcomeScript)
            
            // Switch to main thread to refresh the UI
            withContext(Dispatchers.Main) {
                refreshUserScripts(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting sample scripts", e)
        }
    }
    
    // Don't forget to clean up observers in onCleared
    override fun onCleared() {
        super.onCleared()
        authManager.currentUserId.removeObserver { } // Remove the observer
    }
} 