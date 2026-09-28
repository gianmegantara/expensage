package online.expensage.android.data

import androidx.core.net.toUri
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class LastExpense(
    val amount: Double,
    val currency: String,
    val displayAmount: String,
    val note: String,
    val category: String? = null,
    val occurredAt: String
)

@Serializable
data class TodaySummary(
    val currency: String,
    val targetAllowance: Double,
    val dailyLimit: Double,
    val dailySpent: Double,
    val dailyBudgetProgressPercent: Double,
    val dailyTimeProgressPercent: Double,
    val lastExpense: LastExpense? = null
)

@Serializable
data class CurrencyTotal(
    val currency: String,
    val totalAmount: Double,
    val expenseCount: Int,
)

@Serializable
data class CategoryTotal(
    val category: String,
    val totalAmount: Double,
    val expenseCount: Int,
)

@Serializable
data class TrendPoint(
    val day: String,
    val totalAmount: Double,
)

@Serializable
data class ReportInsight(
    val kind: String,
    val title: String,
    val detail: String,
)

@Serializable
data class ReportExpense(
    val id: String,
    val amount: Double,
    val currency: String,
    val displayAmount: String,
    val note: String,
    val category: String? = null,
    val occurredAt: String,
)

@Serializable
data class BudgetSlice(
    val limit: Double,
    val total: Double,
)

@Serializable
data class MonthlyBudgetSlice(
    val limit: Double,
    val total: Double,
    val endDate: String,
    val remainingDays: Int,
)

@Serializable
data class BudgetSummary(
    val currency: String,
    val daily: BudgetSlice,
    val weekly: BudgetSlice,
    val monthly: MonthlyBudgetSlice,
    val yearly: BudgetSlice,
)

@Serializable
data class ReportSummary(
    val view: String,
    val timezone: String,
    val generatedAt: String,
    val currency: String,
    val totals: List<CurrencyTotal> = emptyList(),
    val categories: List<CategoryTotal> = emptyList(),
    val budget: BudgetSummary? = null,
    val trend: List<TrendPoint> = emptyList(),
    val insights: List<ReportInsight> = emptyList(),
    val recent: List<ReportExpense> = emptyList(),
)

@Serializable
internal data class ReportSummaryResponse(
    val data: ReportSummary? = null,
    val error: String? = null,
)

@Serializable
internal data class UpdateExpenseRequest(
    val note: String,
    val amount: String,
    val currency: String,
    val category: String,
    val occurredAt: String,
)

@Serializable
internal data class MutationResponse(
    val ok: Boolean = false,
    val expense: ReportExpense? = null,
    val error: String? = null,
)

@Serializable
data class SubmitExpenseResult(
  val note: String,
  val displayAmount: String,
  val reportUrl: String,
  val currency: String? = null,
)

@Serializable
data class UserSettings(
    val monthlyBudget: Double,
    val budgetStartDay: Int,
    val defaultCurrency: String
)

@Serializable
internal data class SubmitExpenseRequest(
    val text: String,
    val amount: String,
    val clientNow: String
)

@Serializable
internal data class SubmitExpenseResponse(
    val expense: ExpenseData? = null,
    val reportUrl: String? = null,
    val error: String? = null
)

@Serializable
internal data class ExpenseData(
    val note: String? = null,
    val currency: String? = null,
    val displayAmount: String? = null
)

@Serializable
internal data class TodaySummaryResponse(
    val data: TodaySummary? = null,
    val error: String? = null
)

@Serializable
internal data class SettingsResponse(
    val settings: UserSettings? = null,
    val error: String? = null
)

@Serializable
internal data class CurrenciesResponse(
    val currencies: List<String> = emptyList()
)

@Serializable
internal data class AuthCheckResponse(
    val email: String? = null,
    val error: String? = null
)

@Serializable
internal data class MagicUrlResponse(
    val url: String? = null,
    val error: String? = null
)

class ApiException(
  override val message: String,
  val statusCode: Int? = null,
) : IOException(message)

class ExpenseApiClient {
  private val client: OkHttpClient get() = sharedClient
  private val jsonParser = Json { 
    ignoreUnknownKeys = true 
    coerceInputValues = true
  }

  suspend fun submitExpense(
    config: SetupConfig,
    note: String,
    amount: String,
    idempotencyKey: String,
  ): SubmitExpenseResult = withContext(Dispatchers.IO) {
    val payload = SubmitExpenseRequest(
        text = note.trim(),
        amount = amount.trim(),
        clientNow = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )

    val request = buildRequest(config.endpointUrl, config.accessKey)
      .addHeader("Idempotency-Key", idempotencyKey)
      .addHeader("X-Request-Id", idempotencyKey)
      .post(jsonParser.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
      .build()

    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<SubmitExpenseResponse>(body)
        val expense = result.expense ?: throw ApiException("Incomplete response from server")
        
        SubmitExpenseResult(
            note = expense.note ?: note.trim(),
            displayAmount = expense.displayAmount ?: "${expense.currency ?: ""} ${amount.trim()}".trim(),
            reportUrl = result.reportUrl ?: throw ApiException("Missing report URL"),
            currency = expense.currency
        )
    }
  }

  suspend fun fetchReportMagicUrl(config: SetupConfig): String = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/reports/magic-url")
    val request = buildRequest(url, config.accessKey).get().build()
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<MagicUrlResponse>(body)
        result.url ?: throw ApiException("Magic URL not found in response")
    }
  }

  suspend fun fetchTodaySummary(config: SetupConfig, timezone: String = "UTC"): TodaySummary = withContext(Dispatchers.IO) {
    val url = Uri.parse(getApiUrl(config.endpointUrl, "/api/reports/today"))
        .buildUpon()
        .appendQueryParameter("timezone", timezone)
        .build()
        .toString()

    val request = buildRequest(url, config.accessKey).get().build()
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<TodaySummaryResponse>(body)
        result.data ?: throw ApiException("Today's summary data missing")
    }
  }

  suspend fun fetchReportSummary(
    config: SetupConfig,
    view: String,
    timezone: String = "UTC",
  ): ReportSummary = withContext(Dispatchers.IO) {
    val url = Uri.parse(getApiUrl(config.endpointUrl, "/api/reports/summary"))
        .buildUpon()
        .appendQueryParameter("view", view)
        .appendQueryParameter("timezone", timezone)
        .build()
        .toString()

    val request = buildRequest(url, config.accessKey).get().build()
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<ReportSummaryResponse>(body)
        result.data ?: throw ApiException("Report summary missing in response")
    }
  }

  suspend fun updateExpense(
    config: SetupConfig,
    expenseId: String,
    note: String,
    amount: String,
    currency: String,
    category: String,
    occurredAt: String,
  ): ReportExpense = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/expenses/${Uri.encode(expenseId)}")
    val payload = UpdateExpenseRequest(
      note = note.trim(),
      amount = amount.trim(),
      currency = currency.trim().uppercase(),
      category = category.trim(),
      occurredAt = occurredAt,
    )
    val request = buildRequest(url, config.accessKey)
      .put(jsonParser.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
      .build()

    executeRequest(request) { body ->
      val result = jsonParser.decodeFromString<MutationResponse>(body)
      result.expense ?: throw ApiException("Expense update failed")
    }
  }

  suspend fun deleteExpense(config: SetupConfig, expenseId: String) = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/expenses/${Uri.encode(expenseId)}")
    val request = buildRequest(url, config.accessKey).delete().build()
    executeRequest(request) { Unit }
  }

  suspend fun fetchSettings(config: SetupConfig): UserSettings = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/settings")
    val request = buildRequest(url, config.accessKey).get().build()
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<SettingsResponse>(body)
        result.settings ?: throw ApiException("Settings missing in response")
    }
  }

  suspend fun updateSettings(config: SetupConfig, settings: UserSettings): UserSettings = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/settings")
    val request = buildRequest(url, config.accessKey)
        .post(jsonParser.encodeToString(settings).toRequestBody(JSON_MEDIA_TYPE))
        .build()
    
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<SettingsResponse>(body)
        result.settings ?: throw ApiException("Failed to update settings")
    }
  }

  suspend fun fetchSupportedCurrencies(config: SetupConfig): List<String> = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/currencies")
    val request = buildRequest(url, "").get().build()
    
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<CurrenciesResponse>(body)
        result.currencies
    }
  }

  suspend fun checkAuth(config: SetupConfig): String = withContext(Dispatchers.IO) {
    val url = getApiUrl(config.endpointUrl, "/api/auth/check")
    val request = buildRequest(url, config.accessKey).get().build()
    
    executeRequest(request) { body ->
        val result = jsonParser.decodeFromString<AuthCheckResponse>(body)
        result.email ?: throw ApiException("Email missing in auth check")
    }
  }

  private fun <T> executeRequest(request: Request, parser: (String) -> T): T {
      return client.newCall(request).execute().use { response ->
          val body = response.body?.string() ?: ""
          if (!response.isSuccessful) {
              throw ApiException(parseError(body, response.code), response.code)
          }
          parser(body)
      }
  }

  private fun buildRequest(url: String, accessKey: String): Request.Builder {
    val builder = Request.Builder().url(url)
    if (accessKey.isNotBlank()) {
        builder.addHeader("Authorization", "Bearer $accessKey")
    }
    return builder
  }

  private fun getApiUrl(endpointUrl: String, path: String): String {
    val uri = endpointUrl.toUri()
    val baseUrl = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}"
    return "$baseUrl$path"
  }

  private fun parseError(body: String, statusCode: Int): String {
    return try {
        val result = jsonParser.decodeFromString<AuthCheckResponse>(body)
        result.error?.ifBlank { null }
    } catch (_: Exception) {
        null
    } ?: when (statusCode) {
        401 -> "This access key is no longer valid. Replace your setup link and try again."
        403 -> "You don't have permission to perform this action."
        404 -> "The ExpenSage endpoint was not found. Check your setup link."
        in 500..599 -> "ExpenSage server is having trouble. Please try again later."
        else -> "ExpenSage error (Code $statusCode)."
    }
  }

  companion object {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val sharedClient: OkHttpClient by lazy {
      OkHttpClient.Builder()
        .dns(object : Dns {
          override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            return addresses.sortedBy { if (it is java.net.Inet4Address) 0 else 1 }
          }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    }
  }
}
