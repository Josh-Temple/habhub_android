package com.habhub.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "habhub_prefs",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "habhub_prefs")) }
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val dayBoundaryHour: Int = 0,
    val fontScaleLevel: FontScaleLevel = FontScaleLevel.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class UserPreferencesRepository(private val context: Context) {
    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            dayBoundaryHour = (prefs[KEY_DAY_BOUNDARY_HOUR] ?: 0).coerceIn(0, 23),
            fontScaleLevel = runCatching {
                FontScaleLevel.valueOf(prefs[KEY_FONT_SCALE_LEVEL] ?: FontScaleLevel.NORMAL.name)
            }.getOrDefault(FontScaleLevel.NORMAL),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM)
        )
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDayBoundaryHour(value: Int) {
        context.dataStore.edit { it[KEY_DAY_BOUNDARY_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun setFontScaleLevel(level: FontScaleLevel) {
        context.dataStore.edit { it[KEY_FONT_SCALE_LEVEL] = level.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_DAY_BOUNDARY_HOUR = intPreferencesKey("day_boundary_hour")
        private val KEY_FONT_SCALE_LEVEL = stringPreferencesKey("font_scale_level")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class FontScaleLevel(val scale: Float) {
    SMALL(0.9f),
    NORMAL(1.0f),
    LARGE(1.15f)
}
