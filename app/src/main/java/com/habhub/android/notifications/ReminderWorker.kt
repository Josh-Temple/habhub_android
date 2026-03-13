package com.habhub.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habhub.android.MainActivity
import com.habhub.android.R
import com.habhub.android.data.HabHubDatabase
import com.habhub.android.repository.UserPreferencesRepository
import com.habhub.android.util.businessDate
import kotlinx.coroutines.flow.first

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.success()
        val title = inputData.getString(KEY_HABIT_TITLE) ?: applicationContext.getString(R.string.app_name)

        val prefs = UserPreferencesRepository(applicationContext).preferencesFlow.first()
        val dayBoundaryHour = prefs.dayBoundaryHour
        val today = businessDate(dayBoundaryHour).toString()
        val dao = HabHubDatabase.getInstance(applicationContext).habitDao()
        if (dao.isHabitCompletedOnDate(habitId, today)) {
            val row = dao.getReminderScheduleRowByHabitId(habitId)
            row?.let { ReminderScheduler(applicationContext).scheduleDailyReminders(listOf(it)) }
            return Result.success()
        }

        if (prefs.notificationsEnabled && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "habit_reminder"
            val channel = NotificationChannel(channelId, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)

            val reminderLink = dao.getReminderScheduleRowByHabitId(habitId)?.reminder_link
            val contentIntent = createContentIntent(reminderLink)

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(applicationContext.getString(R.string.reminder_message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }

        val row = dao.getReminderScheduleRowByHabitId(habitId)
        row?.let {
            ReminderScheduler(applicationContext).scheduleDailyReminders(listOf(it))
        }
        return Result.success()
    }


    private fun createContentIntent(reminderLink: String?): Intent {
        val fallbackIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val link = reminderLink?.trim().orEmpty()
        if (link.isBlank()) {
            return fallbackIntent
        }

        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val canOpenLink = viewIntent.resolveActivity(applicationContext.packageManager) != null
        return if (canOpenLink) viewIntent else fallbackIntent
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_REMINDER_TIME = "reminder_time"
    }
}
