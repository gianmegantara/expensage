package online.expensage.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import online.expensage.android.Constants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.expensage.android.data.ReminderScheduler
import online.expensage.android.data.ThemeMode
import online.expensage.android.data.ThemeStore
import online.expensage.android.ui.MainViewModel
import online.expensage.android.ui.SummaryActivity
import online.expensage.android.ui.components.QuickAddScreen
import online.expensage.android.ui.components.SettingsScreen
import online.expensage.android.ui.components.SetupScreen
import online.expensage.android.ui.theme.ExpenSageTheme

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    viewModel.handleIntent(intent)

    // Keep widgets fresh (and ensure their click intents are registered even
    // if a system-driven update was missed, e.g. after an app update).
    TodaySummaryWidget.refreshAll(this)
    QuickAddWidget.refreshAll(this)
    
    setContent {
      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      var themeMode by remember { mutableStateOf(ThemeStore(context).getMode()) }
      val systemDark = isSystemInDarkTheme()
      val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
      }
      var reminderEnabled by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
      val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
      ) { granted ->
        if (granted) {
          ReminderScheduler.setEnabled(context, true)
          reminderEnabled = true
        }
      }

      ExpenSageTheme(darkTheme = isDarkTheme) {
        ExpenSageApp(
          viewModel = viewModel,
          onOpenReport = { url ->
            val customTabsIntent = CustomTabsIntent.Builder()
              .setShowTitle(true)
              .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(url))
            scope.launch {
              delay(200)
              finish()
            }
          },
          onOpenSummary = {
            context.startActivity(Intent(context, SummaryActivity::class.java))
          },
          onPinShortcut = { pinShortcut() },
          onDismiss = { finish() },
          isDarkTheme = isDarkTheme,
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
              notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              ReminderScheduler.setEnabled(context, enabled)
              reminderEnabled = enabled
            }
          },
          onSyncNow = viewModel::syncNow
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    viewModel.handleIntent(intent)
  }

  private fun pinShortcut() {
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
      val shortcut = ShortcutInfoCompat.Builder(this, "quick_add")
        .setShortLabel("Quick Add")
        .setLongLabel("Quick Add Expense")
        .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
        .setIntent(Intent(this, MainActivity::class.java).apply { 
          action = Constants.ACTION_QUICK_ADD
        })
        .build()
      ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
    }
  }

  companion object {
    const val ACTION_QUICK_ADD = Constants.ACTION_QUICK_ADD
  }
}

@Composable
fun ExpenSageApp(
  viewModel: MainViewModel,
  onOpenReport: (String) -> Unit,
  onOpenSummary: () -> Unit,
  onPinShortcut: () -> Unit,
  onDismiss: () -> Unit,
  isDarkTheme: Boolean,
  themeMode: ThemeMode,
  onThemeChange: (ThemeMode) -> Unit,
  reminderEnabled: Boolean,
  onReminderToggle: (Boolean) -> Unit,
  onSyncNow: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  var isVisible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    isVisible = true
  }

  val closeApp = {
    onDismiss()
    Unit
  }

  val closeEvent by viewModel.closeEvent.collectAsStateWithLifecycle()
  LaunchedEffect(closeEvent) {
    if (closeEvent) {
      closeApp()
    }
  }

  uiState.snackbarMessage?.let { message ->
    LaunchedEffect(message) {
      snackbarHostState.showSnackbar(message)
      viewModel.consumeSnackbar()
    }
  }

  uiState.pendingSetup?.let {
    AlertDialog(
      onDismissRequest = viewModel::dismissPendingSetupReplacement,
      title = { Text(stringResource(R.string.dialog_replace_setup_title)) },
      text = { Text(stringResource(R.string.dialog_replace_setup_text)) },
      confirmButton = {
        TextButton(onClick = viewModel::confirmPendingSetupReplacement) {
          Text(stringResource(R.string.dialog_replace_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissPendingSetupReplacement) {
          Text(stringResource(R.string.dialog_replace_dismiss))
        }
      },
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = closeApp,
      ),
    contentAlignment = Alignment.Center,
  ) {
    // Snappy entrance: Fast fade + high stiffness scale
    AnimatedVisibility(
      visible = isVisible && !uiState.isLoading,
      enter = fadeIn(animationSpec = tween(150)) + scaleIn(spring(dampingRatio = 0.9f, stiffness = 800f), initialScale = 0.95f),
      exit = fadeOut(tween(100))
    ) {
      // Use a static Card style for the first frame
    val elevation = remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
      delay(200)
      elevation.value = 12.dp
    }

    Card(
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .wrapContentHeight()
          .imePadding()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.value),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.ime),
        ) {
          // Top row with Close button
          Box(
            modifier = Modifier.fillMaxWidth()
          ) {
            IconButton(
              onClick = closeApp,
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 8.dp, y = (-12).dp)
                .size(32.dp)
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close_button_description),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))

          // Screen content transition
          AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
              (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200)))
                .togetherWith(fadeOut(tween(150)))
            },
            label = "screen_switch"
          ) { state ->
            when (state) {
              online.expensage.android.ui.Screen.LOADING -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    uiState.loadingText?.let {
                      Spacer(modifier = Modifier.height(16.dp))
                      Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
              online.expensage.android.ui.Screen.SETUP -> {
                SetupScreen(
                  setupLinkInput = uiState.setupLinkInput,
                  onSetupLinkInputChanged = viewModel::onSetupLinkInputChanged,
                  onImportSetup = viewModel::importSetupLinkFromInput,
                )
              }
              online.expensage.android.ui.Screen.SETTINGS -> {
                SettingsScreen(
                  userSettings = uiState.userSettings,
                  supportedCurrencies = uiState.supportedCurrencies,
                  isLoading = uiState.isLoadingSettings,
                  isUpdating = uiState.isUpdatingSettings,
                  onUpdateSettings = viewModel::updateUserSettings,
                  onPinShortcut = onPinShortcut,
                  onClearSetup = {
                    viewModel.clearSavedSetup()
                  },
                  onClose = { viewModel.closeSettings() },
                  themeMode = themeMode,
                  onThemeChange = onThemeChange,
                  reminderEnabled = reminderEnabled,
                  onReminderToggle = onReminderToggle,
                  accountEmail = uiState.accountEmail,
                  pendingCount = uiState.pendingCount,
                  onSyncNow = onSyncNow,
                )
              }
              online.expensage.android.ui.Screen.QUICK_ADD -> {
                QuickAddScreen(
                  note = uiState.note,
                  amount = uiState.amount,
                  isSubmitting = uiState.isSubmitting,
                  success = uiState.success,
                  onNoteChanged = viewModel::onNoteChanged,
                  onAmountChanged = viewModel::onAmountChanged,
                  onSubmit = viewModel::submitExpense,
                  onDismissSuccess = { viewModel.dismissSuccess(false) },
                  onOpenReport = onOpenReport,
                  onRequestReport = onOpenSummary,
                  isOnline = uiState.isOnline,
                  isSyncing = uiState.isSyncing,
                  isFetchingReport = uiState.isFetchingReport
                )
              }
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 32.dp),
    )
  }
}
