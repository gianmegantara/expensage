package online.expensage.android.data

import android.content.Context

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            values().firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMode(): ThemeMode = ThemeMode.fromStorage(prefs.getString(KEY_THEME, null))

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.storageValue).apply()
    }

    companion object {
        private const val PREFS = "expensage_prefs"
        private const val KEY_THEME = "theme_mode"
    }
}
