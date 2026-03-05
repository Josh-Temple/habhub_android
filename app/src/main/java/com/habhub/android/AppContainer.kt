package com.habhub.android

import android.content.Context
import com.habhub.android.data.HabHubDatabase
import com.habhub.android.notifications.ReminderScheduler
import com.habhub.android.repository.HabitRepository
import com.habhub.android.repository.UserPreferencesRepository
import com.habhub.android.ui.HabitViewModelFactory

class AppContainer(context: Context) {
    private val dao = HabHubDatabase.getInstance(context).habitDao()
    private val repo = HabitRepository(dao)
    private val scheduler = ReminderScheduler(context)
    private val preferences = UserPreferencesRepository(context)

    val habitViewModelFactory = HabitViewModelFactory(repo, scheduler, preferences)
}
