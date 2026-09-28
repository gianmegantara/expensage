package online.expensage.android.data

import android.content.Context
import org.json.JSONObject

class WidgetCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(summary: TodaySummary) {
        val json = JSONObject().apply {
            put("currency", summary.currency)
            put("targetAllowance", summary.targetAllowance)
            put("dailyLimit", summary.dailyLimit)
            put("dailySpent", summary.dailySpent)
            put("dailyBudgetProgressPercent", summary.dailyBudgetProgressPercent)
            put("dailyTimeProgressPercent", summary.dailyTimeProgressPercent)
            put("lastUpdated", System.currentTimeMillis())
            
            summary.lastExpense?.let { last ->
                put("lastExpense", JSONObject().apply {
                    put("amount", last.amount)
                    put("currency", last.currency)
                    put("displayAmount", last.displayAmount)
                    put("note", last.note)
                    put("category", last.category)
                    put("occurredAt", last.occurredAt)
                })
            }
        }
        prefs.edit().putString(KEY_SUMMARY, json.toString()).apply()
    }

    fun load(): CachedSummary? {
        val jsonString = prefs.getString(KEY_SUMMARY, null) ?: return null
        return try {
            val json = JSONObject(jsonString)
            
            val lastExpenseJson = json.optJSONObject("lastExpense")
            val lastExpense = if (lastExpenseJson != null) {
                LastExpense(
                    amount = lastExpenseJson.getDouble("amount"),
                    currency = lastExpenseJson.getString("currency"),
                    displayAmount = lastExpenseJson.getString("displayAmount"),
                    note = lastExpenseJson.getString("note"),
                    category = lastExpenseJson.optString("category").ifBlank { null },
                    occurredAt = lastExpenseJson.getString("occurredAt")
                )
            } else null

            CachedSummary(
                summary = TodaySummary(
                    currency = json.getString("currency"),
                    targetAllowance = json.getDouble("targetAllowance"),
                    dailyLimit = json.getDouble("dailyLimit"),
                    dailySpent = json.getDouble("dailySpent"),
                    dailyBudgetProgressPercent = json.getDouble("dailyBudgetProgressPercent"),
                    dailyTimeProgressPercent = json.getDouble("dailyTimeProgressPercent"),
                    lastExpense = lastExpense
                ),
                lastUpdated = json.getLong("lastUpdated")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class CachedSummary(
        val summary: TodaySummary,
        val lastUpdated: Long
    )

    companion object {
        private const val PREFS_NAME = "widget_cache"
        private const val KEY_SUMMARY = "today_summary"
    }
}
