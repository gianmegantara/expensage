package online.expensage.android.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "pending_expenses")
data class PendingExpenseEntity(
    @PrimaryKey val id: String,
    val note: String,
    val amount: String,
    val timestamp: String
)

@Dao
interface PendingExpenseDao {
    @Query("SELECT * FROM pending_expenses ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: PendingExpenseEntity)

    @Query("DELETE FROM pending_expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_expenses")
    suspend fun clearAll()
}

@Database(entities = [PendingExpenseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingExpenseDao(): PendingExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expensage_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        fun setTestInstance(instance: AppDatabase?) {
            INSTANCE = instance
        }
    }
}
