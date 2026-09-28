package online.expensage.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import online.expensage.android.data.ConfigStore
import online.expensage.android.data.ExpenseApiClient
import online.expensage.android.data.WidgetCache
import online.expensage.android.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class TodaySummaryWidget : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiClient = ExpenseApiClient()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, TodaySummaryWidget::class.java)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(componentName)
            
            // Show loading state for all widgets immediately
            for (id in appWidgetIds) {
                showLoadingState(context, appWidgetManager, id)
            }

            val pendingResult = goAsync()
            scope.launch {
                try {
                    for (id in appWidgetIds) {
                        updateSingleWidgetSync(context, appWidgetManager, id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background refresh", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateSingleWidgetSync(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateSingleWidgetSync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val configStore = ConfigStore(context)
        val widgetCache = WidgetCache(context)
        val config = configStore.load()
        
        if (config == null) {
            val cached = widgetCache.load()
            updateAppWidget(context, appWidgetManager, appWidgetId, cached?.summary, cached?.lastUpdated)
            return
        }

        try {
            val summary = apiClient.fetchTodaySummary(config, TimeZone.getDefault().id)
            
            // Update currency clue and cache
            configStore.saveCurrency(summary.currency)
            widgetCache.save(summary)

            updateAppWidget(context, appWidgetManager, appWidgetId, summary, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId from network, trying cache", e)
            val cached = widgetCache.load()
            updateAppWidget(context, appWidgetManager, appWidgetId, cached?.summary, cached?.lastUpdated)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        summary: online.expensage.android.data.TodaySummary?,
        lastUpdated: Long? = null,
        isLoading: Boolean = false
    ) {
        val packageName = context.applicationContext.packageName
        val views = RemoteViews(packageName, R.layout.today_summary_widget)
        
        // Handle loading state UI
        if (isLoading) {
            views.setViewVisibility(R.id.btn_refresh, android.view.View.GONE)
            views.setViewVisibility(R.id.pb_loading, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.btn_refresh, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.pb_loading, android.view.View.GONE)
        }

        if (summary != null) {
            try {
                val spentText = CurrencyFormatter.format(summary.dailySpent, summary.currency)
                val allowanceText = context.getString(R.string.widget_allowance_label, CurrencyFormatter.format(summary.targetAllowance, summary.currency))
                
                // If the API returns progress as a ratio (0.0 to 1.0), multiply by 100
                var progress = summary.dailyBudgetProgressPercent
                if (progress > 0 && progress <= 1.0) {
                    progress *= 100
                }
                val progressInt = progress.toInt()
                val percentageText = "$progressInt%"
                
                views.setTextViewText(R.id.tv_today_spent, spentText)
                views.setTextViewText(R.id.tv_allowance, allowanceText)
                views.setTextViewText(R.id.tv_percentage, percentageText)
                
                // Multi-color logic: 0-70% Green, 70-100% Yellow, >100% Red (over allowance)
                when {
                    progress >= 100 -> {
                        val color = android.graphics.Color.parseColor("#DC2626")
                        views.setTextColor(R.id.tv_today_spent, color) // Red
                        views.setTextColor(R.id.tv_percentage, color)
                        views.setViewVisibility(R.id.pb_budget, android.view.View.GONE)
                        views.setViewVisibility(R.id.pb_budget_yellow, android.view.View.GONE)
                        views.setViewVisibility(R.id.pb_budget_red, android.view.View.VISIBLE)
                        views.setProgressBar(R.id.pb_budget_red, 100, progressInt.coerceIn(0, 100), false)
                    }
                    progress >= 70 -> {
                        val color = android.graphics.Color.parseColor("#F59E0B")
                        views.setTextColor(R.id.tv_today_spent, color) // Yellow
                        views.setTextColor(R.id.tv_percentage, color)
                        views.setViewVisibility(R.id.pb_budget, android.view.View.GONE)
                        views.setViewVisibility(R.id.pb_budget_yellow, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.pb_budget_red, android.view.View.GONE)
                        views.setProgressBar(R.id.pb_budget_yellow, 100, progressInt.coerceIn(0, 100), false)
                    }
                    else -> {
                        val color = android.graphics.Color.parseColor("#006049")
                        views.setTextColor(R.id.tv_today_spent, color) // Green
                        views.setTextColor(R.id.tv_percentage, color)
                        views.setViewVisibility(R.id.pb_budget, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.pb_budget_yellow, android.view.View.GONE)
                        views.setViewVisibility(R.id.pb_budget_red, android.view.View.GONE)
                        views.setProgressBar(R.id.pb_budget, 100, progressInt.coerceIn(0, 100), false)
                    }
                }

                if (summary.lastExpense != null) {
                    val last = summary.lastExpense
                    val lastText = "Recently added: ${last.note} (${last.displayAmount})"
                    views.setTextViewText(R.id.tv_last_expense, lastText)
                    views.setViewVisibility(R.id.tv_last_expense, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.tv_last_expense, android.view.View.GONE)
                }

                if (lastUpdated != null) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    views.setTextViewText(R.id.tv_last_updated, timeFormat.format(Date(lastUpdated)))
                    views.setViewVisibility(R.id.tv_last_updated, android.view.View.VISIBLE)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting summary data", e)
                views.setTextViewText(R.id.tv_today_spent, "Error")
            }
        } else {
            val configStore = ConfigStore(context)
            val currencyCode = configStore.loadCurrency()
            if (currencyCode != null) {
                views.setTextViewText(R.id.tv_today_spent, CurrencyFormatter.format(0.0, currencyCode))
            } else {
                views.setTextViewText(R.id.tv_today_spent, "...")
            }
            views.setTextViewText(R.id.tv_percentage, "")
            views.setViewVisibility(R.id.tv_last_updated, android.view.View.GONE)
        }

        // Set up Open App Intent (quick-add popup)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = online.expensage.android.Constants.ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

        // Set up Refresh Intent
        val refreshIntent = Intent(context, TodaySummaryWidget::class.java).apply {
            action = ACTION_REFRESH
            // Set component explicitly to ensure it reaches this receiver
            setClass(context, TodaySummaryWidget::class.java)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId, // Unique request code per widget
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showLoadingState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetCache = WidgetCache(context)
        val cached = widgetCache.load()
        updateAppWidget(context, appWidgetManager, appWidgetId, cached?.summary, cached?.lastUpdated, isLoading = true)
    }

    companion object {
        private const val TAG = "TodaySummaryWidget"
        const val ACTION_REFRESH = "online.expensage.android.action.REFRESH_WIDGET"

        fun refreshAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, TodaySummaryWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val intent = Intent(context, TodaySummaryWidget::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
}
