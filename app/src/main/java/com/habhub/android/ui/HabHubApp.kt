package com.habhub.android.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.core.net.toUri
import com.habhub.android.R
import com.habhub.android.domain.HabitEditUiModel
import com.habhub.android.domain.HabitUiModel
import com.habhub.android.domain.LinkType
import com.habhub.android.domain.NewHabitInput
import com.habhub.android.domain.appLinkIcon
import com.habhub.android.domain.habitIconOptions
import com.habhub.android.domain.iconForSymbol
import com.habhub.android.domain.webLinkIcon
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class AppTab { TODAY, HABITS, SETTINGS }

private val weekDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private const val linkOpenTestUrl = "https://developer.android.com"

@Composable
fun HabHubApp(
    factory: HabitViewModelFactory,
    useDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val vm: HabitViewModel = viewModel(factory = factory)
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val habits = uiState.items
    val uncompleted = habits.filterNot { it.completedToday }
    val completed = habits.filter { it.completedToday }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEditUiModel?>(null) }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }

    val errorMessage = when (uiState.inputError) {
        HabitInputError.TITLE -> stringResource(R.string.error_title_required)
        HabitInputError.TIME -> stringResource(R.string.error_time_invalid)
        HabitInputError.WEB -> stringResource(R.string.error_web_invalid)
        HabitInputError.APP -> stringResource(R.string.error_app_invalid)
        HabitInputError.START_DATE -> stringResource(R.string.error_start_date_invalid)
        HabitInputError.END_DATE -> stringResource(R.string.error_end_date_invalid)
        HabitInputError.DATE_RANGE -> stringResource(R.string.error_date_range_invalid)
        null -> null
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
                    selected = currentTab == AppTab.HABITS,
                    onClick = { currentTab = AppTab.HABITS },
                    icon = { Icon(Icons.Rounded.List, contentDescription = stringResource(R.string.tab_habits)) },
                    label = { Text(stringResource(R.string.tab_habits)) }
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
            if (currentTab == AppTab.HABITS) {
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

                AppTab.HABITS -> HabitsContent(
                    modifier = Modifier.padding(padding),
                    items = uiState.manageItems,
                    onEdit = { editingHabit = it }
                )

                AppTab.SETTINGS -> SettingsContent(
                    modifier = Modifier.padding(padding),
                    notificationsEnabled = uiState.notificationsEnabled,
                    onNotificationsChange = vm::setNotificationsEnabled,
                    useDarkTheme = useDarkTheme,
                    onThemeChange = onThemeChange,
                    onOpenTestUrl = {
                        runCatching {
                            openLink(context = it, payload = linkOpenTestUrl)
                        }.onFailure {
                            scope.launch { snackbarHostState.showSnackbar(it.localizedMessage ?: "") }
                        }
                    }
                )
            }

            if (showAddDialog) {
                HabitFormDialog(
                    title = stringResource(R.string.add_habit),
                    initial = null,
                    onDismiss = { showAddDialog = false },
                    onSave = {
                        vm.addHabit(it)
                        showAddDialog = false
                    }
                )
            }

            editingHabit?.let { editing ->
                HabitFormDialog(
                    title = stringResource(R.string.edit_habit),
                    initial = editing,
                    onDismiss = { editingHabit = null },
                    onSave = {
                        vm.updateHabit(editing.id, it)
                        editingHabit = null
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
private fun HabitsContent(
    modifier: Modifier = Modifier,
    items: List<HabitEditUiModel>,
    onEdit: (HabitEditUiModel) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(text = stringResource(R.string.tab_habits), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.placeholder_habits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(items, key = { it.id }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = iconForSymbol(row.iconName), contentDescription = row.title)
                        Column {
                            Text(text = row.title)
                            Text(
                                text = "${row.startDate} - ${row.endDate ?: "∞"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(row) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_habit))
                    }
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
    onNotificationsChange: (Boolean) -> Unit,
    useDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onOpenTestUrl: (android.content.Context) -> Unit
) {
    val context = LocalContext.current

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.settings_dark_theme))
            Switch(checked = useDarkTheme, onCheckedChange = onThemeChange)
        }
        OutlinedButton(
            onClick = { onOpenTestUrl(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.settings_link_test_button))
        }
        Text(
            text = stringResource(R.string.settings_link_test_value, linkOpenTestUrl),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.settings_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HabitFormDialog(
    title: String,
    initial: HabitEditUiModel?,
    onDismiss: () -> Unit,
    onSave: (NewHabitInput) -> Unit
) {
    var habitTitle by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var selectedIconName by remember(initial) { mutableStateOf(initial?.iconName ?: habitIconOptions.first().key) }
    var reminderTime by remember(initial) { mutableStateOf(initial?.reminderTime.orEmpty()) }
    var webLink by remember(initial) { mutableStateOf(initial?.webLink.orEmpty()) }
    var appLink by remember(initial) { mutableStateOf(initial?.appLink.orEmpty()) }
    var startDate by remember(initial) { mutableStateOf(initial?.startDate ?: LocalDate.now().toString()) }
    var endDate by remember(initial) { mutableStateOf(initial?.endDate.orEmpty()) }
    var selectedDays by remember(initial) { mutableStateOf(maskToDays(initial?.repeatDaysMask)) }
    var iconMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = habitTitle,
                    onValueChange = { habitTitle = it },
                    placeholder = { Text(text = stringResource(R.string.habit_title_hint)) }
                )

                OutlinedButton(onClick = { iconMenuExpanded = true }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconForSymbol(selectedIconName), contentDescription = selectedIconName)
                        Text(text = selectedIconName)
                    }
                }
                DropdownMenu(expanded = iconMenuExpanded, onDismissRequest = { iconMenuExpanded = false }) {
                    habitIconOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.key) },
                            leadingIcon = { Icon(option.icon, contentDescription = option.key) },
                            onClick = {
                                selectedIconName = option.key
                                iconMenuExpanded = false
                            }
                        )
                    }
                }

                TextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = normalizeTimeInput(it) },
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
                TextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    placeholder = { Text(text = stringResource(R.string.start_date_hint)) }
                )
                TextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    placeholder = { Text(text = stringResource(R.string.end_date_hint)) }
                )

                Text(text = stringResource(R.string.weekday_hint), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekDayLabels.forEachIndexed { index, label ->
                        val checked = selectedDays.contains(index)
                        OutlinedButton(onClick = {
                            selectedDays = if (checked) selectedDays - index else selectedDays + index
                        }) {
                            Text(if (checked) "✓$label" else label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    NewHabitInput(
                        title = habitTitle,
                        iconName = selectedIconName,
                        reminderTime = normalizeTimeInput(reminderTime),
                        webLink = webLink,
                        appLink = appLink,
                        repeatDaysMask = daysToMask(selectedDays),
                        startDate = startDate,
                        endDate = endDate
                    )
                )
            }) { Text(text = stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
        }
    )
}


private fun normalizeTimeInput(value: String): String {
    val raw = value.filter(Char::isDigit).take(4)
    return if (raw.length == 4) {
        val hh = raw.substring(0, 2)
        val mm = raw.substring(2, 4)
        "$hh:$mm"
    } else {
        value
    }
}

private fun openLink(context: android.content.Context, payload: String) {
    val normalizedPayload = payload.trim()
    val intent = if (normalizedPayload.startsWith("intent://", ignoreCase = true)) {
        Intent.parseUri(normalizedPayload, Intent.URI_INTENT_SCHEME)
    } else {
        Intent(Intent.ACTION_VIEW, normalizedPayload.toUri())
    }.apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        return
    }

    if (normalizedPayload.startsWith("intent://", ignoreCase = true)) {
        val browserFallbackUrl = intent.getStringExtra("browser_fallback_url")
        if (!browserFallbackUrl.isNullOrBlank()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, browserFallbackUrl.toUri()).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            if (browserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(browserIntent)
                return
            }
        }
    }

    throw ActivityNotFoundException("No handler for $normalizedPayload")
}

private fun maskToDays(mask: Int?): Set<Int> {
    if (mask == null) return emptySet()
    return buildSet {
        for (i in 0..6) {
            if ((mask and (1 shl i)) != 0) add(i)
        }
    }
}

private fun daysToMask(days: Set<Int>): Int? {
    if (days.isEmpty()) return null
    var mask = 0
    days.forEach { day ->
        mask = mask or (1 shl day)
    }
    return mask
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
                            runCatching { openLink(context = context, payload = link.payload) }
                                .onFailure { onLinkOpenFailed(context.getString(R.string.link_open_failed)) }
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
