package online.expensage.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * Typography aligned with the web app's weight rhythm (Manrope/Instrument Serif).
 * Uses the platform sans-serif family; system fonts keep the APK small.
 */
val ExpenSageTypography = Typography(
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Bold),
)
