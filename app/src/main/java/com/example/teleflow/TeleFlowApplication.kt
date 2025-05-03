package com.example.teleflow

import android.app.Application
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.models.Script
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date

class TeleFlowApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob())
    private lateinit var database: TeleFlowDatabase
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database
        database = TeleFlowDatabase.getDatabase(this)
        
        // Populate the database with sample data if needed
        applicationScope.launch {
            initializeDatabase()
        }
    }
    
    private suspend fun initializeDatabase() {
        val scriptDao = database.scriptDao()
        
        // Check if the database is empty
        val scriptCount = scriptDao.getScriptCount()
        
        // Only insert sample data if the database is empty
        if (scriptCount == 0) {
            // Current time for base timestamp
            val now = Date()
            
            // Create dates for different times
            val fourHoursAgo = Date(now.time - (4 * 3600000))
            val threeHoursAgo = Date(now.time - (3 * 3600000))
            val twoHoursAgo = Date(now.time - (2 * 3600000))
            val oneHourAgo = Date(now.time - (1 * 3600000))
            
            // Create sample scripts with descending lastUsedAt timestamps
            // This will make them appear in reverse order in the recent scripts list
            val sampleScripts = listOf(
                Script(
                    id = 0, // Room will auto-generate the ID
                    title = "Product Introduction",
                    content = "Hello everyone! Today I'm excited to introduce our latest product. This innovative solution addresses many of the challenges our customers have been facing...",
                    createdAt = fourHoursAgo,
                    lastModifiedAt = fourHoursAgo,
                    lastUsedAt = fourHoursAgo
                ),
                Script(
                    id = 0,
                    title = "Weekly Update",
                    content = "Welcome to this week's update. We've made significant progress on several key initiatives. First, the development team completed the new payment gateway integration...",
                    createdAt = twoHoursAgo,
                    lastModifiedAt = twoHoursAgo,
                    lastUsedAt = twoHoursAgo
                ),
                Script(
                    id = 0,
                    title = "Tutorial: Getting Started",
                    content = "Welcome to this tutorial where I'll walk you through getting started with our platform. The first step is to create your account by clicking the sign-up button...",
                    createdAt = threeHoursAgo,
                    lastModifiedAt = threeHoursAgo,
                    lastUsedAt = threeHoursAgo
                ),
                Script(
                    id = 0,
                    title = "Social Media Promo",
                    content = "Hey followers! Don't miss our limited-time offer - 30% off all premium subscriptions when you use code SUMMER30 at checkout. This deal ends Friday!...",
                    createdAt = now,
                    lastModifiedAt = now,
                    lastUsedAt = now
                ),
                Script(
                    id = 0,
                    title = "Meeting Presentation",
                    content = "Good morning team! Today's agenda includes a review of Q2 results, discussion of upcoming product features, and team assignments for the next sprint...",
                    createdAt = oneHourAgo,
                    lastModifiedAt = oneHourAgo,
                    lastUsedAt = oneHourAgo
                )
            )
            scriptDao.insertAll(sampleScripts)
        }
    }
} 