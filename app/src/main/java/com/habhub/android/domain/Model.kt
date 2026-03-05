package com.habhub.android.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class LinkType {
    WEB,
    APP_INTENT
}

data class HabitUiModel(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val colorToken: String?,
    val reminderTime: String?,
    val links: List<HabitLinkUiModel>,
    val completedToday: Boolean
)

data class HabitEditUiModel(
    val id: String,
    val title: String,
    val iconName: String,
    val colorToken: String?,
    val sortOrder: Int,
    val reminderTime: String?,
    val webLink: String?,
    val appLink: String?,
    val isOneTime: Boolean,
    val repeatDaysMask: Int?,
    val startDate: String,
    val endDate: String?
)

data class HabitLinkUiModel(
    val type: LinkType,
    val payload: String
)

data class NewHabitInput(
    val title: String,
    val iconName: String,
    val colorToken: String?,
    val reminderTime: String?,
    val webLink: String?,
    val appLink: String?,
    val isOneTime: Boolean,
    val repeatDaysMask: Int?,
    val startDate: String,
    val endDate: String?
)

data class HabitIconOption(
    val key: String,
    val icon: ImageVector
)

data class HabitColorOption(
    val key: String,
    val color: Color
)

val habitIconOptions: List<HabitIconOption> = listOf(
    HabitIconOption("self_improvement", Icons.Rounded.SelfImprovement),
    HabitIconOption("menu_book", Icons.Rounded.MenuBook),
    HabitIconOption("notifications", Icons.Rounded.Notifications),
    HabitIconOption("fitness_center", Icons.Rounded.FitnessCenter),
    HabitIconOption("directions_run", Icons.Rounded.DirectionsRun),
    HabitIconOption("favorite", Icons.Rounded.Favorite),
    HabitIconOption("school", Icons.Rounded.School),
    HabitIconOption("alarm", Icons.Rounded.Alarm),
    HabitIconOption("timer", Icons.Rounded.Timer),
    HabitIconOption("check_circle", Icons.Rounded.CheckCircle),
    HabitIconOption("calendar_month", Icons.Rounded.CalendarMonth),
    HabitIconOption("psychology", Icons.Rounded.Psychology),
    HabitIconOption("emoji_nature", Icons.Rounded.EmojiNature),
    HabitIconOption("spa", Icons.Rounded.Spa),
    HabitIconOption("sports_basketball", Icons.Rounded.SportsBasketball),
    HabitIconOption("fastfood", Icons.Rounded.Fastfood),
    HabitIconOption("local_drink", Icons.Rounded.LocalDrink),
    HabitIconOption("local_cafe", Icons.Rounded.LocalCafe),
    HabitIconOption("bedtime", Icons.Rounded.Bedtime),
    HabitIconOption("music_note", Icons.Rounded.MusicNote),
    HabitIconOption("pets", Icons.Rounded.Pets),
    HabitIconOption("bookmark", Icons.Rounded.Bookmark),
    HabitIconOption("edit", Icons.Rounded.Edit),
    HabitIconOption("palette", Icons.Rounded.Palette),
    HabitIconOption("brush", Icons.Rounded.Brush),
    HabitIconOption("build", Icons.Rounded.Build),
    HabitIconOption("volunteer_activism", Icons.Rounded.VolunteerActivism),
    HabitIconOption("auto_awesome", Icons.Rounded.AutoAwesome),
    HabitIconOption("code", Icons.Rounded.Code),
    HabitIconOption("mood", Icons.Rounded.Mood)
)

fun iconForSymbol(symbol: String): ImageVector {
    return habitIconOptions.firstOrNull { it.key == symbol }?.icon ?: Icons.Rounded.SelfImprovement
}

val webLinkIcon: ImageVector = Icons.Rounded.OpenInBrowser
val appLinkIcon: ImageVector = Icons.Rounded.OpenInNew

val habitColorOptions: List<HabitColorOption> = listOf(
    HabitColorOption("default", Color.Unspecified),
    HabitColorOption("coral", Color(0xFFE57373)),
    HabitColorOption("amber", Color(0xFFFFB74D)),
    HabitColorOption("leaf", Color(0xFF81C784)),
    HabitColorOption("teal", Color(0xFF4DB6AC)),
    HabitColorOption("sky", Color(0xFF64B5F6)),
    HabitColorOption("violet", Color(0xFF9575CD))
)

fun colorForToken(token: String?): Color {
    if (token.isNullOrBlank() || token == "default") return Color.Unspecified
    return habitColorOptions.firstOrNull { it.key == token }?.color ?: Color.Unspecified
}
