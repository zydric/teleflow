package com.example.teleflow.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.teleflow.auth.AuthManager
import com.example.teleflow.auth.AuthResult
import com.example.teleflow.models.User
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication-related operations.
 * Provides a clean interface for fragments to interact with AuthManager.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = AuthManager(application)
    
    /**
     * LiveData of the current user, can be observed for UI changes
     */
    val currentUser: LiveData<User?> = authManager.currentUser
    
    /**
     * LiveData of the current user ID, can be observed for data filtering
     */
    val currentUserId: LiveData<Int?> = authManager.currentUserId
    
    /**
     * Check if a user is currently logged in
     */
    fun isLoggedIn(): Boolean = authManager.isLoggedIn()
    
    /**
     * Get the current user ID or null if not logged in
     */
    fun getCurrentUserId(): Int? = authManager.getCurrentUserId()
    
    /**
     * Register a new user
     * 
     * @param email User's email address
     * @param fullName User's full name
     * @param password User's password
     * @param callback Callback with result
     */
    fun register(email: String, fullName: String, password: String, callback: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authManager.register(email, fullName, password)
            callback(result)
        }
    }
    
    /**
     * Login an existing user
     * 
     * @param email User's email address
     * @param password User's password
     * @param callback Callback with result
     */
    fun login(email: String, password: String, callback: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authManager.login(email, password)
            callback(result)
        }
    }
    
    /**
     * Logout the current user
     */
    fun logout() {
        authManager.logout()
    }
    
    /**
     * Update user profile information
     * 
     * @param fullName User's full name
     * @param email User's email address
     * @param profileImagePath Optional path to profile image
     * @param callback Callback with result
     */
    fun updateUserProfile(
        fullName: String,
        email: String,
        profileImagePath: String? = null,
        callback: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.updateUserProfile(fullName, email, profileImagePath)
            callback(result)
        }
    }
    
    /**
     * Change user's password
     * 
     * @param currentPassword Current password
     * @param newPassword New password
     * @param callback Callback with result
     */
    fun changePassword(currentPassword: String, newPassword: String, callback: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authManager.changePassword(currentPassword, newPassword)
            callback(result)
        }
    }
    
    /**
     * Refresh current user data from the database
     * Use this when you need to ensure the latest user data is loaded
     */
    fun refreshUserData() {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: return@launch
            val user = authManager.refreshUserData(userId)
            // The AuthManager will update the LiveData automatically
        }
    }
} 