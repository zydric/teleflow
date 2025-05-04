package com.example.teleflow.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.teleflow.data.TeleFlowDatabase
import com.example.teleflow.data.TeleFlowRepository
import com.example.teleflow.models.Script
import com.example.teleflow.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Date

/**
 * Manages user authentication and session handling for the TeleFlow app.
 * Since this is a device-local app, authentication is simple and stored in the local database.
 */
class AuthManager(private val context: Context) {
    
    // Shared preferences to store session information
    private val preferences = context.getSharedPreferences("teleflow_auth", Context.MODE_PRIVATE)
    
    // Database access
    private val database = TeleFlowDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val repository = TeleFlowRepository.getInstance(
        database.scriptDao(),
        database.recordingDao(),
        database.userDao()
    )
    
    // LiveData to observe the current logged-in user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    // Observable user ID
    private val _currentUserId = MutableLiveData<Int?>()
    val currentUserId: LiveData<Int?> = _currentUserId
    
    // CoroutineScope for database operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        // Load current user from shared preferences if available
        loadCurrentUser()
    }
    
    /**
     * Load the current user from shared preferences and update LiveData
     */
    private fun loadCurrentUser() {
        val userId = preferences.getInt("current_user_id", -1)
        if (userId != -1) {
            _currentUserId.postValue(userId)
            coroutineScope.launch {
                val user = userDao.getUserByIdSync(userId)
                _currentUser.postValue(user)
            }
        } else {
            _currentUserId.postValue(null)
            _currentUser.postValue(null)
        }
    }
    
    /**
     * Register a new user
     * 
     * @param email User's email address
     * @param fullName User's full name
     * @param password User's password (will be hashed before storage)
     * @return Result with success state and message
     */
    suspend fun register(email: String, fullName: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user already exists
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    return@withContext AuthResult(false, "Email already registered")
                }
                
                // Hash the password
                val passwordHash = hashPassword(password)
                
                // Create new user
                val user = User(
                    email = email,
                    fullName = fullName,
                    passwordHash = passwordHash,
                    profileImagePath = null,
                    createdAt = Date()
                )
                
                // Insert into database
                val userId = userDao.insert(user).toInt()
                
                // Create a welcome script for the new user
                createWelcomeScript(userId)
                
                // Set as current user
                setCurrentUser(userId)
                
                return@withContext AuthResult(true, "Registration successful")
            } catch (e: Exception) {
                return@withContext AuthResult(false, "Registration failed: ${e.message}")
            }
        }
    }
    
    /**
     * Login an existing user
     * 
     * @param email User's email address
     * @param password User's password (will be hashed and compared)
     * @return Result with success state and message
     */
    suspend fun login(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Hash the password
                val passwordHash = hashPassword(password)
                
                // Check credentials
                val user = userDao.login(email, passwordHash)
                
                if (user != null) {
                    // Update last login time
                    userDao.updateLastLogin(user.id, Date())
                    
                    // Save current user
                    setCurrentUser(user.id)
                    
                    return@withContext AuthResult(true, "Login successful")
                } else {
                    return@withContext AuthResult(false, "Invalid email or password")
                }
            } catch (e: Exception) {
                return@withContext AuthResult(false, "Login failed: ${e.message}")
            }
        }
    }
    
    /**
     * Logout the current user
     */
    fun logout() {
        preferences.edit().remove("current_user_id").apply()
        _currentUser.postValue(null)
        _currentUserId.postValue(null)
    }
    
    /**
     * Get the current user ID or null if not logged in
     */
    fun getCurrentUserId(): Int? {
        val userId = preferences.getInt("current_user_id", -1)
        return if (userId != -1) userId else null
    }
    
    /**
     * Check if a user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return getCurrentUserId() != null
    }
    
    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(
        fullName: String,
        email: String,
        profileImagePath: String? = null
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext AuthResult(false, "Not logged in")
                
                // Get current user
                val currentUser = userDao.getUserByIdSync(userId) ?: 
                    return@withContext AuthResult(false, "User not found")
                
                // Check if email is being changed and if it's already taken
                if (email != currentUser.email) {
                    val existingUser = userDao.getUserByEmail(email)
                    if (existingUser != null && existingUser.id != userId) {
                        return@withContext AuthResult(false, "Email already in use")
                    }
                }
                
                // Update user
                val updatedUser = currentUser.copy(
                    fullName = fullName,
                    email = email,
                    profileImagePath = profileImagePath ?: currentUser.profileImagePath
                )
                
                userDao.update(updatedUser)
                
                // Update the current user LiveData
                _currentUser.postValue(updatedUser)
                
                return@withContext AuthResult(true, "Profile updated successfully")
            } catch (e: Exception) {
                return@withContext AuthResult(false, "Update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Change user's password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext AuthResult(false, "Not logged in")
                
                // Get current user
                val user = userDao.getUserByIdSync(userId) ?: 
                    return@withContext AuthResult(false, "User not found")
                
                // Verify current password
                val currentPasswordHash = hashPassword(currentPassword)
                
                // Debug logging - to be removed after fix is confirmed
                android.util.Log.d("AuthManager", "Current password validation:")
                android.util.Log.d("AuthManager", "Input hash: $currentPasswordHash")
                android.util.Log.d("AuthManager", "Stored hash: ${user.passwordHash}")
                
                // Directly validate with database to avoid any hash comparison issues
                val userWithPassword = userDao.login(user.email, currentPasswordHash)
                if (userWithPassword == null) {
                    return@withContext AuthResult(false, "Current password is incorrect")
                }
                
                // Hash new password
                val newPasswordHash = hashPassword(newPassword)
                
                // Update user
                val updatedUser = user.copy(passwordHash = newPasswordHash)
                userDao.update(updatedUser)
                
                return@withContext AuthResult(true, "Password changed successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext AuthResult(false, "Password change failed: ${e.message}")
            }
        }
    }
    
    /**
     * Set the current user ID in shared preferences and update LiveData
     */
    private fun setCurrentUser(userId: Int) {
        preferences.edit().putInt("current_user_id", userId).apply()
        _currentUserId.postValue(userId)
        
        coroutineScope.launch {
            val user = userDao.getUserByIdSync(userId)
            _currentUser.postValue(user)
        }
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
    
    /**
     * Creates a welcome script for a newly registered user
     */
    private suspend fun createWelcomeScript(userId: Int) {
        try {
            // Check if the user already has scripts (avoid duplicates)
            val scriptCount = repository.getUserScriptCount(userId)
            if (scriptCount > 0) {
                // User already has scripts, don't create a welcome script
                return
            }
            
            val now = Date()
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
        } catch (e: Exception) {
            // Log error but don't fail registration if script creation fails
            e.printStackTrace()
        }
    }
}

/**
 * Authentication result data class
 */
data class AuthResult(val success: Boolean, val message: String) 