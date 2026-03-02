package com.habhub.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitScheduleEntity::class,
        HabitLinkEntity::class,
        CompletionLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HabHubDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN is_one_time INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS completion_logs (
                        id TEXT NOT NULL,
                        habit_id TEXT NOT NULL,
                        local_date TEXT NOT NULL,
                        completed_at_epoch_ms INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(habit_id) REFERENCES habits(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_completion_logs_habit_id ON completion_logs(habit_id)"
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_completion_logs_habit_id_local_date
                    ON completion_logs(habit_id, local_date)
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: HabHubDatabase? = null

        fun getInstance(context: Context): HabHubDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabHubDatabase::class.java,
                    "habhub.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
