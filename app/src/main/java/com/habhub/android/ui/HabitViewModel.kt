package com.habhub.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.habhub.android.data.DailyCompletionRow
import com.habhub.android.domain.HabitUiModel
import com.habhub.android.domain.NewHabitInput
import com.habhub.android.notifications.ReminderScheduler
import com.habhub.android.repository.HabitRepository
import com.habhub.android.util.LinkValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val items: List<HabitUiModel> = emptyList(),
    val history: List<DailyCompletionRow> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val inputError: String? = null
)

class HabitViewModel(
    private val repo: HabitRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.seedInitialDataIfNeeded()
            scheduler.scheduleDailyReminders(repo.getReminderSchedules())
        }

        viewModelScope.launch {
            repo.observeTodayHabits().collect { list ->
                _uiState.update { it.copy(items = list, isLoading = false) }
            }
        }

        viewModelScope.launch {
            repo.observeDailyCompletion().collect { rows ->
                _uiState.update { it.copy(history = rows) }
            }
        }
    }

    fun onCompletionToggle(habitId: String, checked: Boolean) {
        viewModelScope.launch {
            repo.toggleCompletion(habitId, checked)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun clearInputError() {
        _uiState.update { it.copy(inputError = null) }
    }

    fun addHabit(input: NewHabitInput) {
        viewModelScope.launch {
            val title = input.title.trim()
            if (title.isBlank()) {
                _uiState.update { it.copy(inputError = "title") }
                return@launch
            }
            if (!input.reminderTime.isNullOrBlank() && !LinkValidator.isValidTime(input.reminderTime)) {
                _uiState.update { it.copy(inputError = "time") }
                return@launch
            }
            if (!input.webLink.isNullOrBlank() && !LinkValidator.isValidWebUrl(input.webLink)) {
                _uiState.update { it.copy(inputError = "web") }
                return@launch
            }
            if (!input.appLink.isNullOrBlank() && !LinkValidator.isValidAppLink(input.appLink)) {
                _uiState.update { it.copy(inputError = "app") }
                return@launch
            }

            repo.addHabit(
                input.copy(
                    title = title,
                    reminderTime = input.reminderTime?.takeIf { it.isNotBlank() },
                    webLink = input.webLink?.takeIf { it.isNotBlank() },
                    appLink = input.appLink?.takeIf { it.isNotBlank() }
                )
            )

            scheduler.scheduleDailyReminders(repo.getReminderSchedules())
            _uiState.update { it.copy(inputError = null) }
        }
    }
}

class HabitViewModelFactory(
    private val repo: HabitRepository,
    private val scheduler: ReminderScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repo, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
