package com.habhub.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habhub.android.R
import com.habhub.android.data.HabHubDatabase

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.success()
        val title = inputData.getString(KEY_HABIT_TITLE) ?: applicationContext.getString(R.string.app_name)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "habit_reminder"
            val channel = NotificationChannel(channelId, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(applicationContext.getString(R.string.reminder_message))
                .setAutoCancel(true)
                .build()

            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }

        val row = HabHubDatabase.getInstance(applicationContext)
            .habitDao()
            .getReminderScheduleRowByHabitId(habitId)
        row?.let {
            ReminderScheduler(applicationContext).scheduleDailyReminders(listOf(it))
        }
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_REMINDER_TIME = "reminder_time"
    }
}
