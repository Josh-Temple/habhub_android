package com.habhub.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index(value = ["sort_order"])]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "icon_name") val iconName: String,
    @ColumnInfo(name = "color_token") val colorToken: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @ColumnInfo(name = "is_one_time") val isOneTime: Boolean
)

@Entity(
    tableName = "habit_schedules",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"])]
)
data class HabitScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "repeat_type") val repeatType: String,
    @ColumnInfo(name = "repeat_days_mask") val repeatDaysMask: Int?,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean,
    @ColumnInfo(name = "reminder_time_local") val reminderTimeLocal: String?,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?
)

@Entity(
    tableName = "habit_links",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"])]
)
data class HabitLinkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "link_type") val linkType: String,
    val title: String?,
    @ColumnInfo(name = "url_or_intent") val urlOrIntent: String,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "open_mode") val openMode: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long
)

@Entity(
    tableName = "completion_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id"]),
        Index(value = ["habit_id", "local_date"], unique = true)
    ]
)
data class CompletionLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "completed_at_epoch_ms") val completedAtEpochMs: Long,
    val source: String
)
