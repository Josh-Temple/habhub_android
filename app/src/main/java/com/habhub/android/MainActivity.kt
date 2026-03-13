package com.habhub.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.habhub.android.ui.HabHubApp
import com.habhub.android.ui.HabitViewModel
import com.habhub.android.ui.theme.HabHubTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = AppContainer(this)
        val viewModel = ViewModelProvider(this, appContainer.habitViewModelFactory)[HabitViewModel::class.java]

        lifecycleScope.launch {
            viewModel.permissionRequests.collect {
                requestNotificationPermissionIfNeeded()
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HabHubTheme(themeMode = uiState.themeMode) {
                HabHubApp(
                    vm = viewModel
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
