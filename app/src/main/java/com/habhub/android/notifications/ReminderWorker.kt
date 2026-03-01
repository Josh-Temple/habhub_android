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

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.success()
        val title = inputData.getString(KEY_HABIT_TITLE) ?: applicationContext.getString(R.string.app_name)
        val time = inputData.getString(KEY_REMINDER_TIME) ?: return Result.success()

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

        ReminderScheduler(applicationContext).scheduleNextDay(habitId = habitId, title = title, time = time)
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_REMINDER_TIME = "reminder_time"
    }
}
