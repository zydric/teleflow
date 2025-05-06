package com.example.teleflow.data

import androidx.lifecycle.LiveData
import com.example.teleflow.data.dao.RecordingDao
import com.example.teleflow.data.dao.ScriptDao
import com.example.teleflow.data.dao.UserDao
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.models.User
import java.util.Date

class TeleFlowRepository(
    private val scriptDao: ScriptDao,
    private val recordingDao: RecordingDao,
    private val userDao: UserDao
) {
    // Scripts
    val allScripts: LiveData<List<Script>> = scriptDao.getAllScripts()
    val recentlyUsedScripts: LiveData<List<Script>> = scriptDao.getRecentlyUsedScripts()

    fun getUserScripts(userId: Int): LiveData<List<Script>> {
        return scriptDao.getUserScripts(userId)
    }
    
    suspend fun getUserScriptsSync(userId: Int): List<Script> {
        return scriptDao.getUserScriptsSync(userId)
    }
    
    fun getUserRecentlyUsedScripts(userId: Int): LiveData<List<Script>> {
        return scriptDao.getRecentlyUsedScripts(userId)
    }
    
    suspend fun insertScript(script: Script): Long {
        return scriptDao.insert(script)
    }
    
    suspend fun insertAllScripts(scripts: List<Script>) {
        scriptDao.insertAll(scripts)
    }
    
    fun getScriptById(id: Int): LiveData<Script> {
        return scriptDao.getScriptById(id)
    }
    
    suspend fun updateScript(script: Script) {
        scriptDao.update(script)
    }
    
    suspend fun deleteScript(script: Script) {
        scriptDao.delete(script)
    }
    
    suspend fun updateScriptLastUsed(id: Int) {
        // Update the lastUsedAt timestamp
        scriptDao.updateLastUsed(id, Date())
    }

    suspend fun getUserScriptCount(userId: Int): Int {
        return scriptDao.getUserScriptCount(userId)
    }
    
    // Recordings
    val allRecordings: LiveData<List<Recording>> = recordingDao.getAllRecordings()
    
    fun getUserRecordings(userId: Int): LiveData<List<Recording>> {
        return recordingDao.getUserRecordings(userId)
    }
    
    fun getRecentUserRecordings(userId: Int, count: Int): LiveData<List<Recording>> {
        return recordingDao.getRecentUserRecordings(userId, count)
    }
    
    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insert(recording)
    }
    
    suspend fun insertAllRecordings(recordings: List<Recording>) {
        recordingDao.insertAll(recordings)
    }
    
    fun getRecordingById(id: Int): LiveData<Recording> {
        return recordingDao.getRecordingById(id)
    }
    
    fun getRecordingsByScriptId(scriptId: Int): LiveData<List<Recording>> {
        return recordingDao.getRecordingsByScriptId(scriptId)
    }

    fun getRecordingsByScriptIdAndUserId(scriptId: Int, userId: Int): LiveData<List<Recording>> {
        return recordingDao.getRecordingsByScriptIdAndUserId(scriptId, userId)
    }
    
    suspend fun updateRecording(recording: Recording) {
        recordingDao.update(recording)
    }
    
    suspend fun deleteRecording(recording: Recording) {
        recordingDao.delete(recording)
    }
    
    // Users
    fun getAllUsers(): LiveData<List<User>> {
        return userDao.getAllUsers()
    }
    
    fun getUserById(id: Int): LiveData<User> {
        return userDao.getUserById(id)
    }
    
    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
    
    suspend fun getUserByIdSync(id: Int): User? {
        return userDao.getUserByIdSync(id)
    }
    
    suspend fun insertUser(user: User): Long {
        return userDao.insert(user)
    }
    
    suspend fun updateUser(user: User) {
        userDao.update(user)
    }
    
    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
    
    suspend fun updateUserLastLogin(userId: Int, date: Date) {
        userDao.updateLastLogin(userId, date)
    }
    
    suspend fun loginUser(email: String, passwordHash: String): User? {
        return userDao.login(email, passwordHash)
    }
    
    // create repo method
    companion object {
        @Volatile
        private var INSTANCE: TeleFlowRepository? = null
        
        fun getInstance(
            scriptDao: ScriptDao,
            recordingDao: RecordingDao,
            userDao: UserDao
        ): TeleFlowRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TeleFlowRepository(scriptDao, recordingDao, userDao)
                INSTANCE = instance
                instance
            }
        }
    }
} 