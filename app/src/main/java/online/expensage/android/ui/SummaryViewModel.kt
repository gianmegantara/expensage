package online.expensage.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import online.expensage.android.data.ConfigStore
import online.expensage.android.data.ExpenseApiClient
import online.expensage.android.data.ReportExpense
import online.expensage.android.data.ReportSummary
import online.expensage.android.data.SetupConfig
import java.util.TimeZone

val EXPENSE_CATEGORIES = listOf(
    "food", "drink", "groceries", "transport", "shopping", "bills", "health", "entertainment", "other",
)

enum class SummaryPeriod(val apiValue: String, val label: String) {
    TODAY("today", "Today"),
    WEEK("week", "Week"),
    MONTH("month", "Month"),
    YEAR("year", "Year"),
}

data class SummaryUiState(
    val isLoading: Boolean = true,
    val summary: ReportSummary? = null,
    val error: String? = null,
    val offline: Boolean = false,
    val hasConfig: Boolean = true,
    val isFetchingReport: Boolean = false,
    val currencies: List<String> = emptyList(),
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val configStore = ConfigStore(application.applicationContext)
    private val apiClient = ExpenseApiClient()

    private var config: SetupConfig? = null

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val _period = MutableStateFlow(SummaryPeriod.MONTH)
    val period: StateFlow<SummaryPeriod> = _period.asStateFlow()

    init {
        viewModelScope.launch {
            config = configStore.load()
            val current = config
            if (current == null) {
                _uiState.update { it.copy(isLoading = false, hasConfig = false) }
            } else {
                load(_period.value)
                runCatching { apiClient.fetchSupportedCurrencies(current) }
                    .onSuccess { currencies -> _uiState.update { it.copy(currencies = currencies) } }
            }
        }
    }

    fun selectPeriod(period: SummaryPeriod) {
        _period.value = period
        load(period)
    }

    fun retry() = load(_period.value)

    private fun load(period: SummaryPeriod) {
        val current = config ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val summary = apiClient.fetchReportSummary(current, period.apiValue, TimeZone.getDefault().id)
                _uiState.update { it.copy(isLoading = false, summary = summary, error = null, offline = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Could not load report",
                        offline = it.summary != null,
                    )
                }
            }
        }
    }

    fun updateExpense(
        expense: ReportExpense,
        note: String,
        amount: String,
        currency: String,
        category: String,
        onDone: () -> Unit,
    ) {
        val current = config ?: return
        viewModelScope.launch {
            try {
                apiClient.updateExpense(current, expense.id, note, amount, currency, category, expense.occurredAt)
                onDone()
                load(_period.value)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Could not update expense") }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val current = config ?: return
        viewModelScope.launch {
            try {
                apiClient.deleteExpense(current, expenseId)
                load(_period.value)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Could not delete expense") }
            }
        }
    }

    fun openFullReport(onUrl: (String) -> Unit) {
        val current = config ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingReport = true) }
            try {
                val url = apiClient.fetchReportMagicUrl(current)
                onUrl(url)
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Could not open the full report") }
            } finally {
                _uiState.update { it.copy(isFetchingReport = false) }
            }
        }
    }
}
