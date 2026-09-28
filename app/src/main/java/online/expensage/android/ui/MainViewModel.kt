package online.expensage.android.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.math.BigDecimal
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import online.expensage.android.R
import online.expensage.android.data.ApiException
import online.expensage.android.data.ConfigStore
import online.expensage.android.data.ExpenseApiClient
import online.expensage.android.data.PendingExpenseStore
import online.expensage.android.data.PendingSetupConfig
import online.expensage.android.data.SetupConfig
import online.expensage.android.data.SetupLinkParser
import online.expensage.android.data.SyncWorker
import online.expensage.android.data.UserSettings
import online.expensage.android.data.WidgetCache
import online.expensage.android.TodaySummaryWidget
import online.expensage.android.util.ConnectivityObserver
import online.expensage.android.util.CurrencyFormatter
import online.expensage.android.util.NetworkConnectivityObserver
import org.json.JSONException

data class SuccessState(
  val note: String,
  val displayAmount: String,
  val reportUrl: String?,
  val isOffline: Boolean = false,
)

enum class Screen {
  SETUP,
  QUICK_ADD,
  SETTINGS,
  LOADING
}

data class MainUiState(
  val isLoading: Boolean = true,
  val loadingText: String? = null,
  val config: SetupConfig? = null,
  val pendingSetup: PendingSetupConfig? = null,
  val setupLinkInput: String = "",
  val note: String = "",
  val amount: String = "",
  val isSubmitting: Boolean = false,
  val isFetchingReport: Boolean = false,
  val success: SuccessState? = null,
  val snackbarMessage: String? = null,
  val isOnline: Boolean = true,
  val isQuickAddFlow: Boolean = false,
  val userSettings: UserSettings? = null,
  val isLoadingSettings: Boolean = false,
  val isUpdatingSettings: Boolean = false,
  val supportedCurrencies: List<String> = emptyList(),
  val currentScreen: Screen = Screen.QUICK_ADD,
  val isSyncing: Boolean = false,
  val accountEmail: String? = null,
  val pendingCount: Int = 0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val configStore = ConfigStore(application.applicationContext)
  private val pendingStore = PendingExpenseStore(application.applicationContext)
  private val apiClient = ExpenseApiClient()
  private val workManager = WorkManager.getInstance(application)
  private val connectivityObserver = NetworkConnectivityObserver(application)

  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private val _closeEvent = MutableStateFlow(false)
  val closeEvent: StateFlow<Boolean> = _closeEvent.asStateFlow()

  init {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val config = configStore.load()

      val cachedSettings = configStore.loadUserSettings()
      val cachedCurrencies = configStore.loadSupportedCurrencies()
      _uiState.update { 
        val nextScreen = if (config == null) Screen.SETUP else Screen.QUICK_ADD
        it.copy(
          config = config,
          isLoading = false,
          userSettings = cachedSettings,
          supportedCurrencies = cachedCurrencies,
          currentScreen = nextScreen
        )
      }
    }

    viewModelScope.launch {
      connectivityObserver.observe().collect { status ->
        _uiState.update { it.copy(isOnline = status == ConnectivityObserver.Status.Available) }
      }
    }
  }

  private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

  fun handleIntent(intent: Intent?) {
    if (intent?.action == "online.expensage.android.QUICK_ADD") {
      _uiState.update { it.copy(isQuickAddFlow = true, success = null, currentScreen = Screen.QUICK_ADD) }
      return
    }

    _uiState.update { it.copy(isQuickAddFlow = false) }

    val data = intent?.data ?: return
    if (intent.action != Intent.ACTION_VIEW) {
      return
    }

    val rawLink = data.toString()
    
    runCatching { SetupLinkParser.parse(rawLink) }
      .onSuccess { parsed ->
        val currentConfig = _uiState.value.config
        
        // If there's no config, we'll go straight to verification, so show loading IMMEDIATELY
        if (currentConfig == null) {
          _uiState.update { it.copy(isLoading = true, loadingText = "Verifying setup...", setupLinkInput = rawLink, currentScreen = Screen.LOADING) }
          importSetupLinkFromInput()
          return@onSuccess
        }

        if (currentConfig == parsed) {
          _uiState.update { it.copy(setupLinkInput = rawLink) }
          showSnackbar(getString(R.string.snackbar_setup_already_saved))
          return@onSuccess
        }
        
        // If we are replacing, ask for confirmation
        _uiState.update { it.copy(pendingSetup = PendingSetupConfig(parsed, rawLink)) }
      }
      .onFailure {
        _uiState.update { it.copy(setupLinkInput = rawLink) }
        showSnackbar(it.message ?: getString(R.string.snackbar_setup_import_failed))
      }
  }

  fun onSetupLinkInputChanged(value: String) {
    _uiState.update { it.copy(setupLinkInput = value) }
  }

  fun importSetupLinkFromInput() {
    val input = _uiState.value.setupLinkInput
    runCatching { SetupLinkParser.parse(input) }
      .onSuccess { parsed ->
        viewModelScope.launch {
          _uiState.update { it.copy(isLoading = true, loadingText = "Verifying setup...", currentScreen = Screen.LOADING) }
          val startTime = System.currentTimeMillis()
          try {
            apiClient.checkAuth(parsed)
            
            // Ensure 1.5s minimum loading
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1500) delay(1500 - elapsed)
            
            val currentConfig = _uiState.value.config
            saveConfig(parsed, if (currentConfig == null) getString(R.string.snackbar_setup_saved) else getString(R.string.snackbar_setup_replaced))
          } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 800) delay(800 - elapsed)

            _uiState.update { it.copy(isLoading = false, loadingText = null, currentScreen = if (_uiState.value.config == null) Screen.SETUP else Screen.QUICK_ADD) }
            showSnackbar("Invalid setup link: ${e.message ?: "Could not verify authentication"}")
          }
        }
      }
      .onFailure {
        showSnackbar(it.message ?: getString(R.string.snackbar_setup_import_failed))
      }
  }

  fun openSettings() {
    _uiState.update { it.copy(currentScreen = Screen.SETTINGS) }
    fetchUserSettings()
    fetchAccountEmail()
    refreshPendingCount()
  }

  fun fetchAccountEmail() {
    val config = _uiState.value.config ?: return
    if (_uiState.value.accountEmail != null) return
    viewModelScope.launch {
      runCatching { apiClient.checkAuth(config) }
        .onSuccess { email -> _uiState.update { it.copy(accountEmail = email) } }
    }
  }

  fun refreshPendingCount() {
    viewModelScope.launch {
      val count = runCatching { pendingStore.getAll().size }.getOrDefault(0)
      _uiState.update { it.copy(pendingCount = count) }
    }
  }

  fun syncNow() {
    scheduleSync()
    viewModelScope.launch {
      delay(2000)
      refreshPendingCount()
    }
  }

  fun fetchUserSettings() {
    val config = _uiState.value.config ?: return
    // Don't re-fetch if we already have the data
    if (_uiState.value.userSettings != null && _uiState.value.supportedCurrencies.isNotEmpty()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingSettings = true) }
      try {
        // Fetch both in parallel
        val settingsDeferred = async { apiClient.fetchSettings(config) }
        val currenciesDeferred = async { apiClient.fetchSupportedCurrencies(config) }
        
        val settings = settingsDeferred.await()
        val currencies = currenciesDeferred.await()

        _uiState.update { 
          it.copy(
            userSettings = settings, 
            supportedCurrencies = currencies,
            isLoadingSettings = false
          ) 
        }
        configStore.saveUserSettings(settings)
        configStore.saveSupportedCurrencies(currencies)
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoadingSettings = false) }
        showSnackbar("Offline mode: showing cached settings.")
      }
    }
  }

  fun updateUserSettings(newSettings: UserSettings) {
    val config = _uiState.value.config ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isUpdatingSettings = true) }
      try {
        val updated = apiClient.updateSettings(config, newSettings)
        _uiState.update { it.copy(userSettings = updated, isUpdatingSettings = false) }
        configStore.saveUserSettings(updated)
        
        showSnackbar("Settings updated successfully")
        TodaySummaryWidget.refreshAll(getApplication())
      } catch (e: Exception) {
        _uiState.update { it.copy(isUpdatingSettings = false) }
        showSnackbar("Cannot update settings while offline")
      }
    }
  }

  // ... rest of methods remain similar, but ensure currentScreen is updated in saveConfig
  private fun saveConfig(config: SetupConfig, successMessage: String) {
    configStore.save(config)
    _uiState.update {
      it.copy(
        config = config,
        pendingSetup = null,
        setupLinkInput = "",
        isLoading = false,
        loadingText = null,
        currentScreen = Screen.QUICK_ADD
      )
    }
    showSnackbar(successMessage)
    // Removed fetchUserSettings() from here to prevent background fetch
  }

  fun onNoteChanged(value: String) = _uiState.update { it.copy(note = value) }
  fun onAmountChanged(value: String) = _uiState.update { it.copy(amount = value) }
  fun consumeSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
  fun closeSettings() = _uiState.update { it.copy(currentScreen = Screen.QUICK_ADD) }
  fun dismissPendingSetupReplacement() = _uiState.update { it.copy(pendingSetup = null) }

  fun confirmPendingSetupReplacement() {
    val pending = _uiState.value.pendingSetup ?: return
    _uiState.update { 
      it.copy(
        pendingSetup = null,
        setupLinkInput = pending.rawLink,
        isLoading = true,
        loadingText = "Verifying setup...",
        currentScreen = Screen.LOADING
      )
    }
    importSetupLinkFromInput()
  }

  fun clearSavedSetup() {
    configStore.clear()
    WidgetCache(getApplication()).clear()
    _uiState.update {
      it.copy(
        config = null,
        pendingSetup = null,
        setupLinkInput = "",
        userSettings = null,
        accountEmail = null,
        pendingCount = 0,
        currentScreen = Screen.SETUP
      )
    }
    showSnackbar(getString(R.string.snackbar_setup_removed))
  }

  fun submitExpense() {
    val currentState = _uiState.value
    val config = currentState.config ?: return
    val note = currentState.note.trim()
    val amount = currentState.amount.trim()

    if (note.isEmpty() || amount.toBigDecimalOrNull() == null) return

    val requestId = java.util.UUID.randomUUID().toString()
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true) }
      val startTime = System.currentTimeMillis()
      if (!currentState.isOnline) {
        saveOfflineAndShowSuccess(note, amount, requestId)
        return@launch
      }
      try {
        val result = kotlinx.coroutines.withTimeoutOrNull(10000) {
          apiClient.submitExpense(config, note, amount, requestId)
        }

        if (result != null) {
          val elapsed = System.currentTimeMillis() - startTime
          if (elapsed < 800) delay(800 - elapsed)
          _uiState.update {
            it.copy(note = "", amount = "", isSubmitting = false, success = SuccessState(result.note, result.displayAmount, result.reportUrl))
          }
          TodaySummaryWidget.refreshAll(getApplication())
        } else {
          // If it takes more than 10s, fallback to offline mode.
          // Since the server might have already received it, we rely on idempotency during sync.
          saveOfflineAndShowSuccess(note, amount, requestId)
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isSubmitting = false) }
        showSnackbar(e.message ?: "Error")
      }
    }
  }

  private suspend fun saveOfflineAndShowSuccess(note: String, amount: String, requestId: String) {
    pendingStore.enqueue(note, amount, requestId)
    scheduleSync()
    
    // Explicit delay so user sees the "Saving offline..." loading indicator
    delay(1000)

    val displayAmount = CurrencyFormatter.format(amount, configStore.loadCurrency() ?: "IDR")
    _uiState.update {
      it.copy(note = "", amount = "", isSubmitting = false, success = SuccessState(note, displayAmount, null, true))
    }
    TodaySummaryWidget.refreshAll(getApplication())
  }

  private fun scheduleSync() {
    val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
    
    // Track Sync State
    _uiState.update { it.copy(isSyncing = true) }
    
    workManager.enqueue(request)
    
    viewModelScope.launch {
      workManager.getWorkInfoByIdFlow(request.id).collect { info ->
        if (info != null && info.state.isFinished) {
          _uiState.update { it.copy(isSyncing = false) }
        }
      }
    }
  }

  fun fetchReportUrl(onSuccess: (String) -> Unit) {
    val config = _uiState.value.config ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isFetchingReport = true) }
      try {
        val url = apiClient.fetchReportMagicUrl(config)
        onSuccess(url)
      } finally {
        _uiState.update { it.copy(isFetchingReport = false) }
      }
    }
  }

  fun dismissSuccess(autoClose: Boolean) {
    if (autoClose) _closeEvent.update { true }
    else _uiState.update { it.copy(success = null) }
  }

  private fun showSnackbar(m: String) = _uiState.update { it.copy(snackbarMessage = m) }
}
