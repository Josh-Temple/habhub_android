package com.habhub.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habhub.android.R
import com.habhub.android.data.DailyCompletionRow
import com.habhub.android.domain.HabitUiModel
import com.habhub.android.domain.LinkType
import com.habhub.android.domain.NewHabitInput
import com.habhub.android.domain.appLinkIcon
import com.habhub.android.domain.webLinkIcon
import kotlinx.coroutines.launch

private enum class AppTab { TODAY, HISTORY, SETTINGS }

@Composable
fun HabHubApp(factory: HabitViewModelFactory) {
    val vm: HabitViewModel = viewModel(factory = factory)
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val habits = uiState.items
    val uncompleted = habits.filterNot { it.completedToday }
    val completed = habits.filter { it.completedToday }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }

    val errorMessage = when (uiState.inputError) {
        "title" -> stringResource(R.string.error_title_required)
        "time" -> stringResource(R.string.error_time_invalid)
        "web" -> stringResource(R.string.error_web_invalid)
        "app" -> stringResource(R.string.error_app_invalid)
        null -> null
        else -> stringResource(R.string.error_unknown)
    }

    LaunchedEffect(errorMessage) {
        errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(errorMessage)
        vm.clearInputError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == AppTab.TODAY,
                    onClick = { currentTab = AppTab.TODAY },
                    icon = { Icon(Icons.Rounded.Today, contentDescription = stringResource(R.string.tab_today)) },
                    label = { Text(stringResource(R.string.tab_today)) }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { currentTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.tab_history)) },
                    label = { Text(stringResource(R.string.tab_history)) }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { currentTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.TODAY) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_habit))
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                AppTab.TODAY -> TodayContent(
                    isLoading = uiState.isLoading,
                    uncompleted = uncompleted,
                    completed = completed,
                    padding = padding,
                    onToggle = { id, checked -> vm.onCompletionToggle(id, checked) },
                    onLinkOpenFailed = {
                        scope.launch { snackbarHostState.showSnackbar(it) }
                    }
                )

                AppTab.HISTORY -> HistoryContent(
                    modifier = Modifier.padding(padding),
                    rows = uiState.history
                )

                AppTab.SETTINGS -> SettingsContent(
                    modifier = Modifier.padding(padding),
                    notificationsEnabled = uiState.notificationsEnabled,
                    onNotificationsChange = vm::setNotificationsEnabled
                )
            }

            if (showAddDialog) {
                AddHabitDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = {
                        vm.addHabit(it)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayContent(
    isLoading: Boolean,
    uncompleted: List<HabitUiModel>,
    completed: List<HabitUiModel>,
    padding: PaddingValues,
    onToggle: (String, Boolean) -> Unit,
    onLinkOpenFailed: (String) -> Unit
) {
    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Header()
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.section_uncompleted), uncompleted.size)
            }
            items(uncompleted, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    onToggle = { onToggle(habit.id, !habit.completedToday) },
                    onLinkOpenFailed = onLinkOpenFailed
                )
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionTitle(stringResource(R.string.section_completed), completed.size)
            }
            items(completed, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    onToggle = { onToggle(habit.id, !habit.completedToday) },
                    onLinkOpenFailed = onLinkOpenFailed
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(modifier: Modifier = Modifier, rows: List<DailyCompletionRow>) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(text = stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.placeholder_history),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(rows, key = { it.local_date }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = row.local_date)
                    Text(text = row.completed_count.toString(), color = MaterialTheme.colorScheme.primary)
                }
                Divider(color = Color(0xFFE6E6E6))
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.settings_notifications))
            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
        }
        Text(
            text = stringResource(R.string.settings_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (NewHabitInput) -> Unit) {
    var title by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }
    var webLink by remember { mutableStateOf("") }
    var appLink by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.add_habit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(text = stringResource(R.string.habit_title_hint)) }
                )
                TextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    placeholder = { Text(text = stringResource(R.string.reminder_time_hint)) }
                )
                TextField(
                    value = webLink,
                    onValueChange = { webLink = it },
                    placeholder = { Text(text = stringResource(R.string.web_link_hint)) }
                )
                TextField(
                    value = appLink,
                    onValueChange = { appLink = it },
                    placeholder = { Text(text = stringResource(R.string.app_link_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    NewHabitInput(
                        title = title,
                        reminderTime = reminderTime,
                        webLink = webLink,
                        appLink = appLink
                    )
                )
            }) { Text(text = stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.tab_today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.menu))
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Text(
        text = "$title $count",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun HabitRow(
    habit: HabitUiModel,
    onToggle: () -> Unit,
    onLinkOpenFailed: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val completionIcon = if (habit.completedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked
            Icon(
                modifier = Modifier.clickable { onToggle() },
                imageVector = completionIcon,
                contentDescription = stringResource(R.string.cd_completion_toggle),
                tint = if (habit.completedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = habit.icon,
                contentDescription = stringResource(R.string.cd_habit_icon),
                tint = MaterialTheme.colorScheme.onBackground
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = habit.title, style = MaterialTheme.typography.bodyLarge)
                habit.reminderTime?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (habit.reminderTime != null) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = stringResource(R.string.cd_reminder_enabled),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                habit.links.forEach { link ->
                    val (icon, cdesc) = if (link.type == LinkType.WEB) {
                        webLinkIcon to stringResource(R.string.cd_web_link)
                    } else {
                        appLinkIcon to stringResource(R.string.cd_app_link)
                    }
                    Icon(
                        modifier = Modifier.clickable {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.payload))
                                val resolved = intent.resolveActivity(context.packageManager)
                                if (resolved == null) error("No handler")
                                context.startActivity(intent)
                            }.onFailure {
                                onLinkOpenFailed(context.getString(R.string.link_open_failed))
                            }
                        },
                        imageVector = icon,
                        contentDescription = cdesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider(color = Color(0xFFE6E6E6))
    }
}
