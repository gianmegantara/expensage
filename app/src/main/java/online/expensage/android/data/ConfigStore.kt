package online.expensage.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ConfigStore(context: Context) {
  private val prefs by lazy {
    EncryptedSharedPreferences.create(
      context,
      PREFS_NAME,
      MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  fun load(): SetupConfig? {
    val endpointUrl = prefs.getString(KEY_ENDPOINT_URL, null)?.trim().orEmpty()
    val accessKey = prefs.getString(KEY_ACCESS_KEY, null)?.trim().orEmpty()
    if (endpointUrl.isBlank() || accessKey.isBlank()) {
      return null
    }

    return SetupConfig(
      endpointUrl = endpointUrl,
      accessKey = accessKey,
    )
  }

  fun save(config: SetupConfig) {
    prefs.edit()
      .putString(KEY_ENDPOINT_URL, config.endpointUrl.trim())
      .putString(KEY_ACCESS_KEY, config.accessKey.trim())
      .apply()
  }

  fun saveCurrency(currency: String) {
    prefs.edit().putString(KEY_CURRENCY, currency).apply()
  }

  fun loadCurrency(): String? {
    return prefs.getString(KEY_CURRENCY, null)
  }

  fun saveUserSettings(settings: UserSettings) {
    prefs.edit()
      .putLong(KEY_BUDGET, settings.monthlyBudget.toLong())
      .putInt(KEY_START_DAY, settings.budgetStartDay)
      .putString(KEY_CURRENCY, settings.defaultCurrency)
      .apply()
  }

  fun loadUserSettings(): UserSettings? {
    if (!prefs.contains(KEY_BUDGET)) return null
    return UserSettings(
      monthlyBudget = prefs.getLong(KEY_BUDGET, 0L).toDouble(),
      budgetStartDay = prefs.getInt(KEY_START_DAY, 1),
      defaultCurrency = prefs.getString(KEY_CURRENCY, "IDR") ?: "IDR"
    )
  }

  fun saveSupportedCurrencies(currencies: List<String>) {
    prefs.edit().putStringSet(KEY_SUPPORTED_CURRENCIES, currencies.toSet()).apply()
  }

  fun loadSupportedCurrencies(): List<String> {
    return prefs.getStringSet(KEY_SUPPORTED_CURRENCIES, null)?.toList() ?: emptyList()
  }

  fun clear() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_NAME = "expensage_companion"
    private const val KEY_ENDPOINT_URL = "endpoint_url"
    private const val KEY_ACCESS_KEY = "access_key"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_BUDGET = "budget"
    private const val KEY_START_DAY = "start_day"
    private const val KEY_SUPPORTED_CURRENCIES = "supported_currencies"
  }
}
