package com.habhub.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class TodayHabitRow(
    val id: String,
    val title: String,
    val icon_name: String,
    val sort_order: Int,
    val reminder_time_local: String?,
    val completed_flag: Int
)

data class DailyCompletionRow(
    val local_date: String,
    val completed_count: Int
)

@Dao
interface HabitDao {
    @Query(
        """
        SELECT
          h.id AS id,
          h.title AS title,
          h.icon_name AS icon_name,
          h.sort_order AS sort_order,
          s.reminder_time_local AS reminder_time_local,
          CASE WHEN c.id IS NULL THEN 0 ELSE 1 END AS completed_flag
        FROM habits h
        LEFT JOIN completion_logs c
          ON c.habit_id = h.id
          AND c.local_date = :today
        LEFT JOIN habit_schedules s
          ON s.habit_id = h.id
          AND s.reminder_enabled = 1
        WHERE h.is_archived = 0
        ORDER BY completed_flag ASC, h.sort_order ASC
        """
    )
    fun observeTodayHabits(today: String): Flow<List<TodayHabitRow>>

    @Query("SELECT * FROM habit_links WHERE habit_id IN (:habitIds) ORDER BY sort_order ASC")
    suspend fun getLinksForHabits(habitIds: List<String>): List<HabitLinkEntity>

    @Query(
        """
        SELECT local_date, COUNT(*) AS completed_count
        FROM completion_logs
        GROUP BY local_date
        ORDER BY local_date DESC
        LIMIT :limit
        """
    )
    fun observeDailyCompletion(limit: Int = 14): Flow<List<DailyCompletionRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(log: CompletionLogEntity)

    @Query("DELETE FROM completion_logs WHERE habit_id = :habitId AND local_date = :date")
    suspend fun clearCompletion(habitId: String, date: String)

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun countHabits(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(items: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<HabitScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(items: List<HabitLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(item: HabitLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(item: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: HabitScheduleEntity)

    @Query(
        """
        SELECT s.*, h.title AS habit_title
        FROM habit_schedules s
        INNER JOIN habits h ON h.id = s.habit_id
        WHERE s.reminder_enabled = 1 AND s.reminder_time_local IS NOT NULL
        """
    )
    suspend fun getReminderScheduleRows(): List<ReminderScheduleRow>

    @Transaction
    suspend fun seedIfEmpty(
        habits: List<HabitEntity>,
        schedules: List<HabitScheduleEntity>,
        links: List<HabitLinkEntity>
    ) {
        if (countHabits() == 0) {
            insertHabits(habits)
            insertSchedules(schedules)
            insertLinks(links)
        }
    }
}

data class ReminderScheduleRow(
    val id: String,
    val habit_id: String,
    val repeat_type: String,
    val repeat_days_mask: Int?,
    val reminder_enabled: Boolean,
    val reminder_time_local: String?,
    val timezone_id: String,
    val start_date: String,
    val end_date: String?,
    val habit_title: String
)
