package com.example.teleflow

import android.app.Application
import com.example.teleflow.auth.AuthManager
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Script
import com.example.teleflow.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Date

class TeleFlowApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob())
    private lateinit var database: TeleFlowDatabase
    private lateinit var repository: TeleFlowRepository
    
    lateinit var authManager: AuthManager
    
    override fun onCreate() {
        super.onCreate()
        
        database = TeleFlowDatabase.getDatabase(this)
        
        repository = TeleFlowRepository.getInstance(
            database.scriptDao(),
            database.recordingDao(),
            database.userDao()
        )
        
        authManager = AuthManager(this)
        
        applicationScope.launch {
            initializeDatabase()
        }
    }
    
    private suspend fun initializeDatabase() {
        val userCount = repository.getUserCount()
        
        if (userCount == 0) {
            val defaultUser = User(
                id = 0,
                email = "default@teleflow.app",
                fullName = "Default User",
                passwordHash = hashPassword("defaultpass"),
                profileImagePath = null,
                createdAt = Date()
            )
            
            val userId = repository.insertUser(defaultUser).toInt()
            
            addSampleScripts(userId)
        } else {
            val scriptCount = database.scriptDao().getScriptCount()
            
            if (scriptCount == 0) {
                addSampleScripts(1)
            }
        }
    }
    
    private suspend fun addSampleScripts(userId: Int) {
        val scriptCount = repository.getUserScriptCount(userId)
        if (scriptCount > 0) {
            return
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
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 