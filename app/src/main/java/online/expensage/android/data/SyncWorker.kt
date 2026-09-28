package online.expensage.android.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.expensage.android.TodaySummaryWidget

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val store = PendingExpenseStore(context)
    private val configStore = ConfigStore(context)
    private val apiClient = ExpenseApiClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val config = configStore.load() ?: return@withContext Result.failure()
        
        val pendingExpenses = store.getAll()
        if (pendingExpenses.isEmpty()) return@withContext Result.success()

        // Tell the ViewModel/UI we are syncing by updating the count in a cache or preference
        // Since we don't have a direct "syncing" state in UI, we skip adding this complexity for now.

        var allSuccessful = true
        for (expense in pendingExpenses) {
            try {
                apiClient.submitExpense(
                    config = config,
                    note = expense.note,
                    amount = expense.amount,
                    idempotencyKey = expense.id
                )
                store.remove(expense.id)
            } catch (e: ApiException) {
                // If it's a 4xx error (except 429), it's likely a permanent failure
                // (e.g. invalid API key). We should remove it or at least not block others.
                if (e.statusCode in 400..499 && e.statusCode != 429) {
                    store.remove(expense.id) 
                } else {
                    allSuccessful = false
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        if (allSuccessful) {
            TodaySummaryWidget.refreshAll(applicationContext)
            Result.success()
        } else {
            Result.retry()
        }
    }
}
