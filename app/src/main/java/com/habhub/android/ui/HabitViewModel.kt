package com.habhub.android.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.habhub.android.domain.HabitEditUiModel
import com.habhub.android.domain.HabitUiModel
import com.habhub.android.domain.NewHabitInput
import com.habhub.android.notifications.ReminderScheduler
import com.habhub.android.repository.FontScaleLevel
import com.habhub.android.repository.HabitRepository
import com.habhub.android.repository.ThemeMode
import com.habhub.android.repository.UserPreferencesRepository
import com.habhub.android.util.LinkValidator
import com.habhub.android.util.businessDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitInputError {
    TITLE,
    TIME,
    WEB,
    APP,
    START_DATE,
    END_DATE,
    DATE_RANGE
}

data class TodayUiState(
    val items: List<HabitUiModel> = emptyList(),
    val manageItems: List<HabitEditUiModel> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val dayBoundaryHour: Int = 0,
    val fontScaleLevel: FontScaleLevel = FontScaleLevel.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = true,
    val inputError: HabitInputError? = null
)

class HabitViewModel(
    private val repo: HabitRepository,
    private val scheduler: ReminderScheduler,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _permissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val permissionRequests: SharedFlow<Unit> = _permissionRequests.asSharedFlow()

    private var todayObserverJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.preferencesFlow.collect { prefs ->
                val previousBoundary = _uiState.value.dayBoundaryHour
                _uiState.update {
                    it.copy(
                        notificationsEnabled = prefs.notificationsEnabled,
                        dayBoundaryHour = prefs.dayBoundaryHour,
                        fontScaleLevel = prefs.fontScaleLevel,
                        themeMode = prefs.themeMode
                    )
                }
                if (previousBoundary != prefs.dayBoundaryHour) {
                    _uiState.update { it.copy(isLoading = true) }
                    startTodayObservation()
                }
            }
        }

        viewModelScope.launch {
            repo.seedInitialDataIfNeeded()
            scheduler.scheduleDailyReminders(repo.getReminderSchedules())
        }

        startTodayObservation()

        viewModelScope.launch {
            repo.observeManageHabits().collect { list ->
                _uiState.update { it.copy(manageItems = list) }
            }
        }
    }

    fun onCompletionToggle(habitId: String, checked: Boolean) {
        viewModelScope.launch {
            repo.toggleCompletion(habitId, checked, currentBusinessDate())
        }
    }

    fun setDayBoundaryHour(hour: Int) {
        viewModelScope.launch {
            preferences.setDayBoundaryHour(hour)
        }
    }

    fun setFontScaleLevel(level: FontScaleLevel) {
        viewModelScope.launch {
            preferences.setFontScaleLevel(level)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun moveHabit(habitId: String, direction: Int) {
        viewModelScope.launch {
            val current = uiState.value.manageItems
            val index = current.indexOfFirst { it.id == habitId }
            if (index == -1) return@launch
            val target = index + direction
            if (target !in current.indices) return@launch
            val reordered = current.toMutableList().also { list ->
                val item = list.removeAt(index)
                list.add(target, item)
            }
            repo.updateManageOrder(reordered.map { it.id })
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
            if (enabled) {
                _permissionRequests.tryEmit(Unit)
                scheduler.scheduleDailyReminders(repo.getReminderSchedules())
            } else {
                scheduler.cancelAllReminders()
            }
        }
    }

    fun clearInputError() {
        _uiState.update { it.copy(inputError = null) }
    }

    fun addHabit(input: NewHabitInput) {
        submitHabit(input) { validated -> repo.addHabit(validated) }
    }

    fun updateHabit(habitId: String, input: NewHabitInput) {
        submitHabit(input) { validated -> repo.updateHabit(habitId, validated) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repo.deleteHabit(habitId)
            scheduler.scheduleDailyReminders(repo.getReminderSchedules())
        }
    }

    private fun submitHabit(
        input: NewHabitInput,
        action: suspend (NewHabitInput) -> Unit
    ) {
        viewModelScope.launch {
            val title = input.title.trim()
            val normalizedReminderTime = normalizeReminderTime(input.reminderTime)
            val normalizedWebLink = normalizeWebLink(input.webLink)
            if (title.isBlank()) {
                _uiState.update { it.copy(inputError = HabitInputError.TITLE) }
                return@launch
            }
            if (!normalizedReminderTime.isNullOrBlank() && !LinkValidator.isValidTime(normalizedReminderTime)) {
                _uiState.update { it.copy(inputError = HabitInputError.TIME) }
                return@launch
            }
            if (!normalizedWebLink.isNullOrBlank() && !LinkValidator.isValidWebUrl(normalizedWebLink)) {
                _uiState.update { it.copy(inputError = HabitInputError.WEB) }
                return@launch
            }
            if (!input.appLink.isNullOrBlank() && !LinkValidator.isValidAppLink(input.appLink)) {
                _uiState.update { it.copy(inputError = HabitInputError.APP) }
                return@launch
            }
            if (!isValidDate(input.startDate)) {
                _uiState.update { it.copy(inputError = HabitInputError.START_DATE) }
                return@launch
            }
            if (!input.endDate.isNullOrBlank() && !isValidDate(input.endDate)) {
                _uiState.update { it.copy(inputError = HabitInputError.END_DATE) }
                return@launch
            }
            if (!input.endDate.isNullOrBlank() && LocalDate.parse(input.endDate).isBefore(LocalDate.parse(input.startDate))) {
                _uiState.update { it.copy(inputError = HabitInputError.DATE_RANGE) }
                return@launch
            }

            val normalizedEndDate = if (input.isOneTime) input.startDate else input.endDate?.takeIf { it.isNotBlank() }
            action(
                input.copy(
                    title = title,
                    reminderTime = normalizedReminderTime?.takeIf { it.isNotBlank() },
                    webLink = normalizedWebLink?.takeIf { it.isNotBlank() },
                    appLink = input.appLink?.takeIf { it.isNotBlank() },
                    repeatDaysMask = if (input.isOneTime) null else input.repeatDaysMask,
                    endDate = normalizedEndDate
                )
            )

            if (!normalizedReminderTime.isNullOrBlank()) {
                _permissionRequests.tryEmit(Unit)
            }
            scheduler.scheduleDailyReminders(repo.getReminderSchedules())
            _uiState.update { it.copy(inputError = null) }
        }
    }

    private fun isValidDate(value: String): Boolean {
        return runCatching { LocalDate.parse(value) }.isSuccess
    }

    private fun normalizeReminderTime(value: String?): String? {
        value ?: return null
        val trimmed = value.trim()
        val digits = trimmed.filter(Char::isDigit)
        return if (digits.length == 4) {
            "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
        } else {
            trimmed
        }
    }

    private fun normalizeWebLink(value: String?): String? {
        value ?: return null
        val trimmed = value.trim()
        if (trimmed.isBlank()) return trimmed
        val scheme = Uri.parse(trimmed).scheme
        return if (scheme.isNullOrBlank()) "https://$trimmed" else trimmed
    }

    private fun startTodayObservation() {
        todayObserverJob?.cancel()
        todayObserverJob = viewModelScope.launch {
            repo.observeTodayHabits(today = currentBusinessDate()).collect { list ->
                _uiState.update { it.copy(items = list, isLoading = false) }
            }
        }
    }

    private fun currentBusinessDate(): LocalDate = businessDate(_uiState.value.dayBoundaryHour)
}

class HabitViewModelFactory(
    private val repo: HabitRepository,
    private val scheduler: ReminderScheduler,
    private val preferences: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repo, scheduler, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
