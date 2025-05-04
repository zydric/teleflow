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
    
    // Expose AuthManager to be accessed from activities/fragments
    lateinit var authManager: AuthManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database
        database = TeleFlowDatabase.getDatabase(this)
        
        // Initialize repository
        repository = TeleFlowRepository.getInstance(
            database.scriptDao(),
            database.recordingDao(),
            database.userDao()
        )
        
        // Initialize AuthManager
        authManager = AuthManager(this)
        
        // Populate the database with sample data if needed
        applicationScope.launch {
            initializeDatabase()
        }
    }
    
    private suspend fun initializeDatabase() {
        // Check if there are any users
        val userCount = repository.getUserCount()
        
        // If no users exist, create a default user
        if (userCount == 0) {
            // Create default user
            val defaultUser = User(
                id = 0, // Room will auto-generate
                email = "default@teleflow.app",
                fullName = "Default User",
                passwordHash = hashPassword("defaultpass"),
                profileImagePath = null,
                createdAt = Date()
            )
            
            // Insert the default user
            val userId = repository.insertUser(defaultUser).toInt()
            
            // Now add sample scripts for this user
            addSampleScripts(userId)
        } else {
            // Check if there are scripts using the scriptDao directly
            val scriptCount = database.scriptDao().getScriptCount()
            
            // If there are users but no scripts, we might need to add scripts
            // for the default user (ID 1) which was created in migration
            if (scriptCount == 0) {
                addSampleScripts(1) // Default user ID is 1
            }
        }
    }
    
    private suspend fun addSampleScripts(userId: Int) {
        // Check if the user already has scripts (avoid duplicates)
        val scriptCount = repository.getUserScriptCount(userId)
        if (scriptCount > 0) {
            // User already has scripts, don't create a welcome script
            return
        }
        
        // Current time for base timestamp
        val now = Date()
        
        // Create a welcome script for the default user
        val welcomeScript = Script(
            id = 0, // Room will auto-generate the ID
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
        
        // Insert the welcome script
        repository.insertScript(welcomeScript)
    }
    
    /**
     * Hash a password using SHA-256
     * Note: In a production app, you might want to use more secure methods like BCrypt
     */
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 