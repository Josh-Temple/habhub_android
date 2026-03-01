package com.habhub.android.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

enum class LinkType {
    WEB,
    APP_INTENT
}

data class HabitUiModel(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val reminderTime: String?,
    val links: List<HabitLinkUiModel>,
    val completedToday: Boolean
)

data class HabitLinkUiModel(
    val type: LinkType,
    val payload: String
)

data class NewHabitInput(
    val title: String,
    val reminderTime: String?,
    val webLink: String?,
    val appLink: String?
)

fun iconForSymbol(symbol: String): ImageVector {
    return when (symbol) {
        "menu_book" -> Icons.Rounded.MenuBook
        "notifications" -> Icons.Rounded.Notifications
        else -> Icons.Rounded.SelfImprovement
    }
}

val webLinkIcon: ImageVector = Icons.Rounded.OpenInBrowser
val appLinkIcon: ImageVector = Icons.Rounded.OpenInNew
