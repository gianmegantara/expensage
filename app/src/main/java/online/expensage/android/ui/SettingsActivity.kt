package online.expensage.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.expensage.android.Constants
import online.expensage.android.MainActivity
import online.expensage.android.R
import online.expensage.android.data.ReminderScheduler
import online.expensage.android.data.ThemeMode
import online.expensage.android.data.ThemeStore
import online.expensage.android.data.UserSettings
import online.expensage.android.ui.components.SettingsPage
import online.expensage.android.ui.components.SettingsScreen
import online.expensage.android.ui.components.settingsPageTitle
import online.expensage.android.ui.theme.ExpenSageTheme

class SettingsActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemeStore(context).getMode()) }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            var reminderEnabled by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    ReminderScheduler.setEnabled(context, true)
                    reminderEnabled = true
                }
            }
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.fetchUserSettings()
                viewModel.fetchAccountEmail()
                viewModel.refreshPendingCount()
            }

            ExpenSageTheme(darkTheme = darkTheme) {
                SettingsScaffold(
                    state = state,
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        ThemeStore(context).setMode(mode)
                        themeMode = mode
                    },
                    reminderEnabled = reminderEnabled,
                    onReminderToggle = { enabled ->
                        val needsPermission = enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            ReminderScheduler.setEnabled(context, enabled)
                            reminderEnabled = enabled
                        }
                    },
                    onUpdateSettings = viewModel::updateUserSettings,
                    onPinShortcut = { pinShortcut() },
                    onClearSetup = {
                        viewModel.clearSavedSetup()
                        finish()
                    },
                    onSyncNow = viewModel::syncNow,
                    onClose = { finish() },
                )
            }
        }
    }

    private fun pinShortcut() {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            val shortcut = ShortcutInfoCompat.Builder(this, "quick_add")
                .setShortLabel(getString(R.string.shortcut_quick_add_short))
                .setLongLabel(getString(R.string.shortcut_quick_add_long))
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(this, MainActivity::class.java).apply {
                        action = Constants.ACTION_QUICK_ADD
                    },
                )
                .build()
            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    state: MainUiState,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    reminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onPinShortcut: () -> Unit,
    onClearSetup: () -> Unit,
    onSyncNow: () -> Unit,
    onClose: () -> Unit,
) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settingsPageTitle(page)) },
                navigationIcon = {
                    IconButton(onClick = { if (page != SettingsPage.ROOT) page = SettingsPage.ROOT else onClose() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close_button_description),
                        )
                    }
                },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SettingsScreen(
                userSettings = state.userSettings,
                supportedCurrencies = state.supportedCurrencies,
                isLoading = state.isLoadingSettings,
                isUpdating = state.isUpdatingSettings,
                onUpdateSettings = onUpdateSettings,
                onPinShortcut = onPinShortcut,
                onClearSetup = onClearSetup,
                onClose = onClose,
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                reminderEnabled = reminderEnabled,
                onReminderToggle = onReminderToggle,
                accountEmail = state.accountEmail,
                pendingCount = state.pendingCount,
                onSyncNow = onSyncNow,
                showCloseButton = false,
                page = page,
                onPageChange = { page = it },
                showHeader = false,
            )
        }
    }
}
