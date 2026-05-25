package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CouponEntity::class,
        SavingsRecordEntity::class,
        ActivityLogEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SaveLoopDatabase : RoomDatabase() {

    abstract fun saveLoopDao(): SaveLoopDao

    companion object {
        @Volatile
        private var INSTANCE: SaveLoopDatabase? = null

        fun getDatabase(context: Context): SaveLoopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SaveLoopDatabase::class.java,
                    "saveloop_database"
                )
                .fallbackToDestructiveMigration() // Prevent crashes if users update app and schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
