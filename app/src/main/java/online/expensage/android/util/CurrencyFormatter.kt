package online.expensage.android.util

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    fun format(amount: Double, currencyCode: String): String {
        // Use Indonesian locale for IDR or system default otherwise
        val locale = if (currencyCode == "IDR") Locale("id", "ID") else Locale.getDefault()
        
        return try {
            val currency = Currency.getInstance(currencyCode)
            val formatter = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                maximumFractionDigits = 0
            }
            
            var result = formatter.format(amount)
            if (currencyCode == "IDR") {
                result = result.replace("Rp", "Rp ")
            }
            result
        } catch (e: Exception) {
            // Fallback to simple string if currency code is invalid
            "$currencyCode $amount"
        }
    }

    fun format(amountString: String, currencyCode: String): String {
        val amount = amountString.toDoubleOrNull() ?: return amountString
        return format(amount, currencyCode)
    }
}
