package online.expensage.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PendingExpense(
    val id: String,
    val note: String,
    val amount: String,
    val timestamp: String
)

class PendingExpenseStore(context: Context) {
    private val dao = AppDatabase.getDatabase(context).pendingExpenseDao()
    private val oldPrefs = context.getSharedPreferences("pending_expenses", Context.MODE_PRIVATE)

    suspend fun enqueue(note: String, amount: String, id: String = UUID.randomUUID().toString()) = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val entity = PendingExpenseEntity(
            id = id,
            note = note,
            amount = amount,
            timestamp = java.time.OffsetDateTime.now().toString()
        )
        dao.insert(entity)
    }

    suspend fun getAll(): List<PendingExpense> = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        dao.getAll().map {
            PendingExpense(
                id = it.id,
                note = it.note,
                amount = it.amount,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        // No need to migrate for remove, but safe to do it
        dao.deleteById(id)
    }

    private suspend fun migrateIfNeeded() {
        val data = oldPrefs.getString("queue", null) ?: return
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val oldExpenses = json.decodeFromString<List<PendingExpense>>(data)
            for (exp in oldExpenses) {
                dao.insert(PendingExpenseEntity(exp.id, exp.note, exp.amount, exp.timestamp))
            }
        } catch (_: Exception) {
        } finally {
            oldPrefs.edit().clear().apply()
        }
    }
}
