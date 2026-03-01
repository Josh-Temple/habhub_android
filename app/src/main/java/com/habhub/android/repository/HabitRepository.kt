package com.habhub.android.repository

import com.habhub.android.data.CompletionLogEntity
import com.habhub.android.data.DailyCompletionRow
import com.habhub.android.data.HabitDao
import com.habhub.android.data.HabitEntity
import com.habhub.android.data.HabitLinkEntity
import com.habhub.android.data.HabitScheduleEntity
import com.habhub.android.data.ReminderScheduleRow
import com.habhub.android.domain.HabitLinkUiModel
import com.habhub.android.domain.HabitUiModel
import com.habhub.android.domain.LinkType
import com.habhub.android.domain.NewHabitInput
import com.habhub.android.domain.iconForSymbol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class HabitRepository(
    private val dao: HabitDao
) {
    fun observeTodayHabits(today: LocalDate = LocalDate.now()): Flow<List<HabitUiModel>> {
        val date = today.toString()
        return dao.observeTodayHabits(date).map { rows ->
            val ids = rows.map { it.id }
            val linksByHabitId = if (ids.isEmpty()) emptyMap() else dao.getLinksForHabits(ids).groupBy { it.habitId }

            rows.map { row ->
                val links = linksByHabitId[row.id].orEmpty().map { link ->
                    HabitLinkUiModel(
                        type = if (link.linkType == "WEB") LinkType.WEB else LinkType.APP_INTENT,
                        payload = link.urlOrIntent
                    )
                }
                HabitUiModel(
                    id = row.id,
                    title = row.title,
                    icon = iconForSymbol(row.icon_name),
                    reminderTime = row.reminder_time_local,
                    links = links,
                    completedToday = row.completed_flag == 1
                )
            }
        }
    }

    fun observeDailyCompletion(limit: Int = 14): Flow<List<DailyCompletionRow>> = dao.observeDailyCompletion(limit)

    suspend fun toggleCompletion(habitId: String, checked: Boolean, localDate: LocalDate = LocalDate.now()) {
        if (checked) {
            dao.upsertCompletion(
                CompletionLogEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    localDate = localDate.toString(),
                    completedAtEpochMs = System.currentTimeMillis(),
                    source = "MANUAL"
                )
            )
        } else {
            dao.clearCompletion(habitId, localDate.toString())
        }
    }

    suspend fun addHabit(input: NewHabitInput) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault().id
        val id = UUID.randomUUID().toString()
        val sortOrder = now.toInt()

        dao.insertHabit(
            HabitEntity(
                id = id,
                title = input.title,
                iconName = "self_improvement",
                colorToken = null,
                sortOrder = sortOrder,
                isArchived = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        dao.insertSchedule(
            HabitScheduleEntity(
                id = UUID.randomUUID().toString(),
                habitId = id,
                repeatType = "DAILY",
                repeatDaysMask = null,
                reminderEnabled = !input.reminderTime.isNullOrBlank(),
                reminderTimeLocal = input.reminderTime,
                timezoneId = zone,
                startDate = LocalDate.now().toString(),
                endDate = null
            )
        )

        input.webLink?.takeIf { it.isNotBlank() }?.let {
            dao.insertLink(
                HabitLinkEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = id,
                    linkType = "WEB",
                    title = null,
                    urlOrIntent = it,
                    packageName = null,
                    openMode = "EXTERNAL_BROWSER",
                    sortOrder = 1,
                    createdAtEpochMs = now
                )
            )
        }
        input.appLink?.takeIf { it.isNotBlank() }?.let {
            dao.insertLink(
                HabitLinkEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = id,
                    linkType = "APP_INTENT",
                    title = null,
                    urlOrIntent = it,
                    packageName = null,
                    openMode = "INTENT",
                    sortOrder = 2,
                    createdAtEpochMs = now
                )
            )
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault().id
        val habits = listOf(
            HabitEntity("1", "Deep breathing", "self_improvement", null, 1, false, now, now),
            HabitEntity("2", "Read 20 min", "menu_book", null, 2, false, now, now),
            HabitEntity("3", "Journal", "notifications", null, 3, false, now, now)
        )
        val schedules = listOf(
            HabitScheduleEntity("s1", "1", "DAILY", null, true, "20:00", zone, LocalDate.now().toString(), null),
            HabitScheduleEntity("s2", "2", "DAILY", null, true, "21:00", zone, LocalDate.now().toString(), null),
            HabitScheduleEntity("s3", "3", "DAILY", null, false, null, zone, LocalDate.now().toString(), null)
        )
        val links = listOf(
            HabitLinkEntity("l1", "1", "APP_INTENT", null, "intent://com.spotify.music", null, "INTENT", 1, now),
            HabitLinkEntity("l2", "2", "WEB", null, "https://developer.android.com", null, "EXTERNAL_BROWSER", 1, now)
        )
        dao.seedIfEmpty(habits, schedules, links)
    }

    suspend fun getReminderSchedules(): List<ReminderScheduleRow> = dao.getReminderScheduleRows()
}
