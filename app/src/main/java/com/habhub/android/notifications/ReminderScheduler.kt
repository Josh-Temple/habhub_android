package com.habhub.android.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habhub.android.data.ReminderScheduleRow
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun scheduleDailyReminders(rows: List<ReminderScheduleRow>) {
        val manager = WorkManager.getInstance(context)
        rows.forEach { row ->
            val time = row.reminder_time_local ?: return@forEach
            scheduleOne(
                manager = manager,
                habitId = row.habit_id,
                title = row.habit_title,
                time = time,
                replace = true
            )
        }
    }

    fun scheduleNextDay(habitId: String, title: String, time: String) {
        val manager = WorkManager.getInstance(context)
        scheduleOne(manager, habitId, title, time, replace = true)
    }

    private fun scheduleOne(
        manager: WorkManager,
        habitId: String,
        title: String,
        time: String,
        replace: Boolean
    ) {
        val delay = nextDelay(time)
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_HABIT_ID, habitId)
            .putString(ReminderWorker.KEY_HABIT_TITLE, title)
            .putString(ReminderWorker.KEY_REMINDER_TIME, time)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        manager.enqueueUniqueWork(
            "reminder_$habitId",
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun nextDelay(time: String): Duration {
        val now = ZonedDateTime.now()
        val targetTime = LocalTime.parse(time)
        var next = now.withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
