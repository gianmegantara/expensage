package online.expensage.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.expensage.android.MainActivity
import online.expensage.android.R
import online.expensage.android.data.ReportExpense
import online.expensage.android.data.ThemeMode
import online.expensage.android.data.ThemeStore
import online.expensage.android.ui.components.BudgetCard
import online.expensage.android.ui.components.CategoryBreakdown
import online.expensage.android.ui.components.ConfirmDeleteDialog
import online.expensage.android.ui.components.EditExpenseDialog
import online.expensage.android.ui.components.InsightCard
import online.expensage.android.ui.components.RecentExpenseRow
import online.expensage.android.ui.components.SectionTitle
import online.expensage.android.ui.components.SummaryCard
import online.expensage.android.ui.components.SummaryPeriodSelector
import online.expensage.android.ui.components.TotalsCard
import online.expensage.android.ui.components.TrendChart
import online.expensage.android.ui.theme.ExpenSageTheme

class SummaryActivity : ComponentActivity() {
    private val viewModel: SummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = when (ThemeStore(this).getMode()) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            ExpenSageTheme(darkTheme = darkTheme) {
                SummaryScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfNeeded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryScreen(viewModel: SummaryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ReportExpense?>(null) }
    var deleting by remember { mutableStateOf<ReportExpense?>(null) }

    val openWebReport: (String) -> Unit = { url ->
        val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
        customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabs.launchUrl(context, Uri.parse(url))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.retry() },
                        enabled = state.hasConfig && !state.isLoading,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.summary_refresh),
                        )
                    }
                    TextButton(
                        onClick = { viewModel.openFullReport(openWebReport) },
                        enabled = !state.isFetchingReport && state.hasConfig,
                    ) {
                        Text(stringResource(R.string.summary_full_report))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.hasConfig) {
                SummaryPeriodSelector(selected = period, onSelect = viewModel::selectPeriod)
                if (state.isLoading && state.summary != null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            when {
                !state.hasConfig -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.summary_no_setup),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                            Text(stringResource(R.string.setup_import_button))
                        }
                    }
                }

                state.isLoading && state.summary == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.summary == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            state.error ?: stringResource(R.string.summary_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = viewModel::retry) {
                            Text(stringResource(R.string.summary_retry))
                        }
                    }
                }

                else -> {
                    val summary = state.summary!!
                    if (state.offline) {
                        Text(
                            stringResource(R.string.summary_offline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    summary.budget?.let { budget ->
                        val (limit, spent, remainingDays) = when (summary.view) {
                            "today" -> Triple(budget.daily.limit, budget.daily.total, null)
                            "week" -> Triple(budget.weekly.limit, budget.weekly.total, null)
                            "year" -> Triple(budget.yearly.limit, budget.yearly.total, null)
                            else -> Triple(budget.monthly.limit, budget.monthly.total, budget.monthly.remainingDays)
                        }
                        BudgetCard(
                            currency = budget.currency,
                            limit = limit,
                            spent = spent,
                            remainingDays = remainingDays,
                        )
                    }

                    TotalsCard(totals = summary.totals)
                    TrendChart(points = summary.trend, currency = summary.currency)
                    CategoryBreakdown(categories = summary.categories, currency = summary.currency)

                    if (summary.insights.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.summary_insights))
                        summary.insights.forEach { insight -> InsightCard(insight) }
                    }

                    SectionTitle(stringResource(R.string.summary_recent))
                    SummaryCard {
                        if (summary.recent.isEmpty()) {
                            Text(
                                stringResource(R.string.summary_no_expenses),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            summary.recent.take(15).forEach { expense ->
                                RecentExpenseRow(
                                    expense = expense,
                                    onEdit = { editing = expense },
                                    onDelete = { deleting = expense },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            currencies = state.currencies,
            onDismiss = { editing = null },
            onSave = { note, amount, currency, category ->
                viewModel.updateExpense(expense, note, amount, currency, category) { editing = null }
            },
        )
    }

    deleting?.let { expense ->
        ConfirmDeleteDialog(
            expense = expense,
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deleteExpense(expense.id)
                deleting = null
            },
        )
    }
}
