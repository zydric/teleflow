package com.example.teleflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.teleflow.data.dao.RecordingDao
import com.example.teleflow.data.dao.ScriptDao
import com.example.teleflow.data.dao.UserDao
import com.example.teleflow.database.Converters
import com.example.teleflow.models.Recording
import com.example.teleflow.models.Script
import com.example.teleflow.models.User

@Database(
    entities = [User::class, Script::class, Recording::class], 
    version = 5, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TeleFlowDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun scriptDao(): ScriptDao
    abstract fun recordingDao(): RecordingDao
    
    companion object {
        @Volatile
        private var INSTANCE: TeleFlowDatabase? = null
        
        // Migration from version 3 to 4 (adding User table and userId to scripts)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create users table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`email` TEXT NOT NULL, " +
                            "`fullName` TEXT NOT NULL, " +
                            "`passwordHash` TEXT NOT NULL, " +
                            "`profileImagePath` TEXT, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`lastLoginAt` INTEGER)"
                )
                
                // Create temporary scripts table with userId column
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scripts_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`userId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`content` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`lastModifiedAt` INTEGER NOT NULL, " +
                            "`lastUsedAt` INTEGER, " +
                            "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                
                // Create a default user
                database.execSQL(
                    "INSERT INTO `users` (`id`, `email`, `fullName`, `passwordHash`, `profileImagePath`, `createdAt`) " +
                            "VALUES (1, 'default@teleflow.app', 'Default User', '0', NULL, " + System.currentTimeMillis() + ")"
                )
                
                // Copy data from scripts to scripts_new, assigning userId = 1 to all scripts
                database.execSQL(
                    "INSERT INTO `scripts_new` (`id`, `userId`, `title`, `content`, `createdAt`, `lastModifiedAt`, `lastUsedAt`) " +
                            "SELECT `id`, 1, `title`, `content`, `createdAt`, `lastModifiedAt`, `lastUsedAt` FROM `scripts`"
                )
                
                // Drop the old scripts table
                database.execSQL("DROP TABLE `scripts`")
                
                // Rename the new scripts table
                database.execSQL("ALTER TABLE `scripts_new` RENAME TO `scripts`")
                
                // Create index on scripts.userId
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_scripts_userId` ON `scripts` (`userId`)")
            }
        }
        
        // Migration from version 4 to 5 (adding userId to recordings)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary recordings table with userId column
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recordings_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`scriptId` INTEGER NOT NULL, " +
                            "`userId` INTEGER NOT NULL, " +
                            "`videoUri` TEXT NOT NULL, " +
                            "`date` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`scriptId`) REFERENCES `scripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                
                // Copy data from recordings to recordings_new
                // For each recording, look up the script's userId and use that for the recording's userId
                database.execSQL(
                    "INSERT INTO `recordings_new` (`id`, `scriptId`, `userId`, `videoUri`, `date`) " +
                            "SELECT r.`id`, r.`scriptId`, s.`userId`, r.`videoUri`, r.`date` " +
                            "FROM `recordings` r JOIN `scripts` s ON r.`scriptId` = s.`id`"
                )
                
                // Drop the old recordings table
                database.execSQL("DROP TABLE `recordings`")
                
                // Rename the new recordings table
                database.execSQL("ALTER TABLE `recordings_new` RENAME TO `recordings`")
                
                // Create indexes for foreign keys
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_scriptId` ON `recordings` (`scriptId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_userId` ON `recordings` (`userId`)")
            }
        }
        
        fun getDatabase(context: Context): TeleFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeleFlowDatabase::class.java,
                    "teleflow_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 