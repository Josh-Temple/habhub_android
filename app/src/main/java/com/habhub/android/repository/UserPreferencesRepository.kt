package com.habhub.android.repository

import android.content.Context

class UserPreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("habhub_prefs", Context.MODE_PRIVATE)

    fun getDayBoundaryHour(): Int = prefs.getInt(KEY_DAY_BOUNDARY_HOUR, 0)

    fun setDayBoundaryHour(value: Int) {
        prefs.edit().putInt(KEY_DAY_BOUNDARY_HOUR, value.coerceIn(0, 23)).apply()
    }

    fun getFontScaleLevel(): String = prefs.getString(KEY_FONT_SCALE_LEVEL, FontScaleLevel.NORMAL.name) ?: FontScaleLevel.NORMAL.name

    fun setFontScaleLevel(level: FontScaleLevel) {
        prefs.edit().putString(KEY_FONT_SCALE_LEVEL, level.name).apply()
    }

    companion object {
        private const val KEY_DAY_BOUNDARY_HOUR = "day_boundary_hour"
        private const val KEY_FONT_SCALE_LEVEL = "font_scale_level"
    }
}

enum class FontScaleLevel(val scale: Float) {
    SMALL(0.9f),
    NORMAL(1.0f),
    LARGE(1.15f)
}
