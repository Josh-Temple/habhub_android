package com.habhub.android.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habhub.android.data.ReminderScheduleRow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun scheduleDailyReminders(rows: List<ReminderScheduleRow>) {
        val manager = WorkManager.getInstance(context)
        rows.forEach { row ->
            val time = row.reminder_time_local ?: return@forEach
            val zone = ZoneId.of(row.timezone_id)
            val delay = nextEligibleDelay(row = row, time = time, zone = zone)
            if (delay == null) {
                manager.cancelUniqueWork("reminder_${row.habit_id}")
                return@forEach
            }
            scheduleOne(
                manager = manager,
                habitId = row.habit_id,
                title = row.habit_title,
                time = time,
                replace = true,
                initialDelay = delay
            )
        }
    }

    fun scheduleNextDay(habitId: String, title: String, time: String) {
        val manager = WorkManager.getInstance(context)
        scheduleOne(
            manager = manager,
            habitId = habitId,
            title = title,
            time = time,
            replace = true,
            initialDelay = nextDelay(time)
        )
    }

    private fun scheduleOne(
        manager: WorkManager,
        habitId: String,
        title: String,
        time: String,
        replace: Boolean,
        initialDelay: Duration
    ) {
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_HABIT_ID, habitId)
            .putString(ReminderWorker.KEY_HABIT_TITLE, title)
            .putString(ReminderWorker.KEY_REMINDER_TIME, time)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
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

    private fun nextEligibleDelay(
        row: ReminderScheduleRow,
        time: String,
        zone: ZoneId
    ): Duration? {
        val now = ZonedDateTime.now(zone)
        val targetTime = LocalTime.parse(time)
        val start = LocalDate.parse(row.start_date)
        val end = row.end_date?.let { LocalDate.parse(it) }

        for (offset in 0..366) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.isBefore(start)) {
                continue
            }
            if (end != null && date.isAfter(end)) {
                return null
            }
            if (!matchesWeekday(row.repeat_days_mask, date)) {
                continue
            }

            var candidate = now.with(date).withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1)
                if (!matchesWeekday(row.repeat_days_mask, candidate.toLocalDate())) {
                    continue
                }
                if (candidate.toLocalDate().isBefore(start)) {
                    continue
                }
                if (end != null && candidate.toLocalDate().isAfter(end)) {
                    return null
                }
            }
            return Duration.between(now, candidate)
        }

        return null
    }

    private fun matchesWeekday(mask: Int?, date: LocalDate): Boolean {
        mask ?: return true
        val index = date.dayOfWeek.value - 1 // Monday=0 .. Sunday=6
        return (mask and (1 shl index)) != 0
    }
}
