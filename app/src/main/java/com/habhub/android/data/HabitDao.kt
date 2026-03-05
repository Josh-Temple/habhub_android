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
    val color_token: String?,
    val sort_order: Int,
    val reminder_time_local: String?,
    val completed_flag: Int
)

data class HabitManageRow(
    val id: String,
    val title: String,
    val icon_name: String,
    val color_token: String?,
    val sort_order: Int,
    val reminder_time_local: String?,
    val is_one_time: Boolean,
    val repeat_days_mask: Int?,
    val start_date: String,
    val end_date: String?
)

@Dao
interface HabitDao {
    @Query(
        """
        SELECT
          h.id AS id,
          h.title AS title,
          h.icon_name AS icon_name,
          h.color_token AS color_token,
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

    @Query(
        """
        SELECT
          h.id AS id,
          h.title AS title,
          h.icon_name AS icon_name,
          h.color_token AS color_token,
          h.sort_order AS sort_order,
          s.reminder_time_local AS reminder_time_local,
          h.is_one_time AS is_one_time,
          s.repeat_days_mask AS repeat_days_mask,
          s.start_date AS start_date,
          s.end_date AS end_date
        FROM habits h
        INNER JOIN habit_schedules s ON s.habit_id = h.id
        WHERE h.is_archived = 0
        ORDER BY h.sort_order ASC
        """
    )
    fun observeManageHabits(): Flow<List<HabitManageRow>>

    @Query("SELECT * FROM habit_links WHERE habit_id IN (:habitIds) ORDER BY sort_order ASC")
    suspend fun getLinksForHabits(habitIds: List<String>): List<HabitLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(log: CompletionLogEntity)

    @Query("DELETE FROM completion_logs WHERE habit_id = :habitId AND local_date = :date")
    suspend fun clearCompletion(habitId: String, date: String)

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun countHabits(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM completion_logs WHERE habit_id = :habitId AND local_date = :date)")
    suspend fun isHabitCompletedOnDate(habitId: String, date: String): Boolean

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

    @Query("UPDATE habits SET sort_order = :sortOrder, updated_at_epoch_ms = :updatedAtEpochMs WHERE id = :habitId")
    suspend fun updateHabitSortOrder(habitId: String, sortOrder: Int, updatedAtEpochMs: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: HabitScheduleEntity)

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("DELETE FROM habit_links WHERE habit_id = :habitId")
    suspend fun deleteLinksByHabitId(habitId: String)

    @Query("SELECT id FROM habit_schedules WHERE habit_id = :habitId LIMIT 1")
    suspend fun getScheduleIdForHabit(habitId: String): String?


    @Query("DELETE FROM completion_logs WHERE habit_id = :habitId")
    suspend fun deleteCompletionsByHabitId(habitId: String)

    @Query("DELETE FROM habit_schedules WHERE habit_id = :habitId")
    suspend fun deleteScheduleByHabitId(habitId: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Transaction
    suspend fun deleteHabitWithRelations(habitId: String) {
        deleteCompletionsByHabitId(habitId)
        deleteLinksByHabitId(habitId)
        deleteScheduleByHabitId(habitId)
        deleteHabitById(habitId)
    }

    @Transaction
    suspend fun updateHabitWithRelations(
        habit: HabitEntity,
        schedule: HabitScheduleEntity,
        links: List<HabitLinkEntity>
    ) {
        insertHabit(habit)
        insertSchedule(schedule)
        deleteLinksByHabitId(habit.id)
        links.forEach { insertLink(it) }
    }

    @Query(
        """
        SELECT
          s.*, 
          h.title AS habit_title,
          (
            SELECT l.url_or_intent
            FROM habit_links l
            WHERE l.habit_id = s.habit_id
            ORDER BY l.sort_order ASC
            LIMIT 1
          ) AS reminder_link
        FROM habit_schedules s
        INNER JOIN habits h ON h.id = s.habit_id
        WHERE s.reminder_enabled = 1 AND s.reminder_time_local IS NOT NULL
        """
    )
    suspend fun getReminderScheduleRows(): List<ReminderScheduleRow>

    @Query(
        """
        SELECT
          s.*, 
          h.title AS habit_title,
          (
            SELECT l.url_or_intent
            FROM habit_links l
            WHERE l.habit_id = s.habit_id
            ORDER BY l.sort_order ASC
            LIMIT 1
          ) AS reminder_link
        FROM habit_schedules s
        INNER JOIN habits h ON h.id = s.habit_id
        WHERE s.habit_id = :habitId
        LIMIT 1
        """
    )
    suspend fun getReminderScheduleRowByHabitId(habitId: String): ReminderScheduleRow?

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
    val habit_title: String,
    val reminder_link: String?
)
