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

class AuthManager(private val context: Context) {
    private val preferences = context.getSharedPreferences("teleflow_auth", Context.MODE_PRIVATE)

    private val database = TeleFlowDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val repository = TeleFlowRepository.getInstance(
        database.scriptDao(),
        database.recordingDao(),
        database.userDao()
    )

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _currentUserId = MutableLiveData<Int?>()
    val currentUserId: LiveData<Int?> = _currentUserId

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        loadCurrentUser()
    }

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

    suspend fun register(email: String, fullName: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // if user already exists
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    return@withContext AuthResult(false, "Email already registered")
                }

                val passwordHash = hashPassword(password)

                val user = User(
                    email = email,
                    fullName = fullName,
                    passwordHash = passwordHash,
                    profileImagePath = null,
                    createdAt = Date()
                )
                
                // insert to db
                val userId = userDao.insert(user).toInt()

                createWelcomeScript(userId)
                
                return@withContext AuthResult(true, "Registration successful")
            } catch (e: Exception) {
                return@withContext AuthResult(false, "Registration failed: ${e.message}")
            }
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val passwordHash = hashPassword(password)

                val user = userDao.login(email, passwordHash)
                
                if (user != null) {
                    userDao.updateLastLogin(user.id, Date())

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

    fun logout() {
        preferences.edit().remove("current_user_id").apply()
        _currentUser.postValue(null)
        _currentUserId.postValue(null)
    }

    fun getCurrentUserId(): Int? {
        val userId = preferences.getInt("current_user_id", -1)
        return if (userId != -1) userId else null
    }

    fun isLoggedIn(): Boolean {
        return getCurrentUserId() != null
    }

    suspend fun updateUserProfile(
        fullName: String,
        email: String,
        profileImagePath: String? = null
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext AuthResult(false, "Not logged in")

                val currentUser = userDao.getUserByIdSync(userId) ?: 
                    return@withContext AuthResult(false, "User not found")

                if (email != currentUser.email) {
                    val existingUser = userDao.getUserByEmail(email)
                    if (existingUser != null && existingUser.id != userId) {
                        return@withContext AuthResult(false, "Email already in use")
                    }
                }

                val updatedUser = currentUser.copy(
                    fullName = fullName,
                    email = email,
                    profileImagePath = profileImagePath ?: currentUser.profileImagePath
                )
                
                userDao.update(updatedUser)

                _currentUser.postValue(updatedUser)
                
                return@withContext AuthResult(true, "Profile updated successfully")
            } catch (e: Exception) {
                return@withContext AuthResult(false, "Update failed: ${e.message}")
            }
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId() ?: return@withContext AuthResult(false, "Not logged in")

                val user = userDao.getUserByIdSync(userId) ?: 
                    return@withContext AuthResult(false, "User not found")

                val currentPasswordHash = hashPassword(currentPassword)

                val userWithPassword = userDao.login(user.email, currentPasswordHash)
                if (userWithPassword == null) {
                    return@withContext AuthResult(false, "Current password is incorrect")
                }

                val newPasswordHash = hashPassword(newPassword)

                val updatedUser = user.copy(passwordHash = newPasswordHash)
                userDao.update(updatedUser)
                
                return@withContext AuthResult(true, "Password changed successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext AuthResult(false, "Password change failed: ${e.message}")
            }
        }
    }

    private fun setCurrentUser(userId: Int) {
        preferences.edit().putInt("current_user_id", userId).apply()
        _currentUserId.postValue(userId)
        
        coroutineScope.launch {
            val user = userDao.getUserByIdSync(userId)
            _currentUser.postValue(user)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private suspend fun createWelcomeScript(userId: Int) {
        try {
            // if user already has scripts, then no welcome script
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshUserData(userId: Int): User? {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUserByIdSync(userId)
                _currentUser.postValue(user)
                return@withContext user
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}

data class AuthResult(val success: Boolean, val message: String) 