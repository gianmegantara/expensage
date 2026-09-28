package online.expensage.android.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class PendingExpenseStoreTest {

    private lateinit var db: AppDatabase
    private lateinit var store: PendingExpenseStore
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestInstance(db)
        
        store = PendingExpenseStore(context)
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.setTestInstance(null)
    }

    @Test
    fun `test enqueue saves to database`() = runBlocking {
        val id = UUID.randomUUID().toString()
        store.enqueue("Coffee", "5.50", id)
        
        val all = store.getAll()
        assertEquals(1, all.size)
        assertEquals("Coffee", all[0].note)
        assertEquals("5.50", all[0].amount)
        assertEquals(id, all[0].id)
    }

    @Test
    fun `test remove from database`() = runBlocking {
        val id = "123"
        store.enqueue("Lunch", "15.00", id)
        
        assertEquals(1, store.getAll().size)
        
        store.remove(id)
        
        assertEquals(0, store.getAll().size)
    }

    @Test
    fun `test migration from shared preferences`() = runBlocking {
        val oldPrefs = context.getSharedPreferences("pending_expenses", Context.MODE_PRIVATE)
        val json = "[{\"id\":\"migrate-1\",\"note\":\"Legacy\",\"amount\":\"10.0\",\"timestamp\":\"2023-01-01T00:00:00Z\"}]"
        oldPrefs.edit().putString("queue", json).commit()
        
        val all = store.getAll()
        assertEquals(1, all.size)
        assertEquals("Legacy", all[0].note)
        assertEquals("migrate-1", all[0].id)
        
        // Verify prefs cleared
        assertTrue(oldPrefs.getString("queue", null) == null)
    }
}
