package com.example.teleflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.teleflow.data.dao.RecordingDao
import com.example.teleflow.data.dao.ScriptDao
import com.example.teleflow.database.Converters
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script

@Database(entities = [Script::class, Recording::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TeleFlowDatabase : RoomDatabase() {
    
    abstract fun scriptDao(): ScriptDao
    abstract fun recordingDao(): RecordingDao
    
    companion object {
        @Volatile
        private var INSTANCE: TeleFlowDatabase? = null
        
        fun getDatabase(context: Context): TeleFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeleFlowDatabase::class.java,
                    "teleflow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 