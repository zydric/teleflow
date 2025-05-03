package com.example.teleflow.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScriptViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TeleFlowRepository
    val allScripts: LiveData<List<Script>>
    val recentlyUsedScripts: LiveData<List<Script>>
    
    init {
        val database = TeleFlowDatabase.getDatabase(application)
        val scriptDao = database.scriptDao()
        val recordingDao = database.recordingDao()
        repository = TeleFlowRepository(scriptDao, recordingDao)
        allScripts = repository.allScripts
        recentlyUsedScripts = repository.recentlyUsedScripts
    }
    
    fun getScriptById(id: Int): LiveData<Script> {
        return repository.getScriptById(id)
    }
    
    fun insertScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertScript(script)
    }
    
    fun updateScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateScript(script)
    }
    
    fun deleteScript(script: Script) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteScript(script)
    }
    
    fun updateScriptLastUsed(scriptId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateScriptLastUsed(scriptId)
    }
    
    // Function to initialize the database with sample scripts if needed
    fun insertSampleScripts() = viewModelScope.launch(Dispatchers.IO) {
        val sampleScripts = listOf(
            Script(
                id = 0, // Room will auto-generate the ID
                title = "Product Introduction",
                content = "Hello everyone! Today I'm excited to introduce our latest product. This innovative solution addresses many of the challenges our customers have been facing..."
            ),
            Script(
                id = 0,
                title = "Weekly Update",
                content = "Welcome to this week's update. We've made significant progress on several key initiatives. First, the development team completed the new payment gateway integration..."
            ),
            Script(
                id = 0,
                title = "Tutorial: Getting Started",
                content = "Welcome to this tutorial where I'll walk you through getting started with our platform. The first step is to create your account by clicking the sign-up button..."
            ),
            Script(
                id = 0,
                title = "Social Media Promo",
                content = "Hey followers! Don't miss our limited-time offer - 30% off all premium subscriptions when you use code SUMMER30 at checkout. This deal ends Friday!..."
            ),
            Script(
                id = 0,
                title = "Meeting Presentation",
                content = "Good morning team! Today's agenda includes a review of Q2 results, discussion of upcoming product features, and team assignments for the next sprint..."
            )
        )
        repository.insertAllScripts(sampleScripts)
    }
} 