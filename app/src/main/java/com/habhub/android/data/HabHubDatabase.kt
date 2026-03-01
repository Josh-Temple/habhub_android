package com.habhub.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitScheduleEntity::class,
        HabitLinkEntity::class,
        CompletionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HabHubDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var instance: HabHubDatabase? = null

        fun getInstance(context: Context): HabHubDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabHubDatabase::class.java,
                    "habhub.db"
                ).build().also { instance = it }
            }
        }
    }
}
