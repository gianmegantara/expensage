package online.expensage.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import online.expensage.android.R
import online.expensage.android.data.ThemeMode
import online.expensage.android.data.UserSettings

enum class SettingsPage {
    ROOT,
    BUDGET,
    THEME,
    DANGER_ZONE
}

@Composable
fun settingsPageTitle(page: SettingsPage): String = when (page) {
    SettingsPage.ROOT -> stringResource(R.string.settings_title)
    SettingsPage.BUDGET -> "Budget & Currency"
    SettingsPage.THEME -> stringResource(R.string.settings_appearance)
    SettingsPage.DANGER_ZONE -> "Danger Zone"
}

@Composable
fun SettingsScreen(
  userSettings: UserSettings?,
  supportedCurrencies: List<String>,
  isLoading: Boolean,
  isUpdating: Boolean,
  onUpdateSettings: (UserSettings) -> Unit,
  onPinShortcut: () -> Unit,
  onClearSetup: () -> Unit,
  onClose: () -> Unit,
  themeMode: ThemeMode,
  onThemeChange: (ThemeMode) -> Unit,
  reminderEnabled: Boolean,
  onReminderToggle: (Boolean) -> Unit,
  accountEmail: String?,
  pendingCount: Int,
  onSyncNow: () -> Unit,
  showCloseButton: Boolean = true,
  page: SettingsPage? = null,
  onPageChange: ((SettingsPage) -> Unit)? = null,
  showHeader: Boolean = true,
) {
  val haptic = LocalHapticFeedback.current
  var internalPage by remember { mutableStateOf(SettingsPage.ROOT) }
  val currentPage = page ?: internalPage
  val goTo: (SettingsPage) -> Unit = { next ->
    internalPage = next
    onPageChange?.invoke(next)
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    AnimatedContent(
      targetState = currentPage,
      transitionSpec = {
          if (targetState != SettingsPage.ROOT) {
              slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
          } else {
              slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
          }
      },
      label = "settings_navigation"
    ) { targetPage ->
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          if (showHeader) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
              ) {
                  if (targetPage != SettingsPage.ROOT) {
                      IconButton(onClick = {
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          goTo(SettingsPage.ROOT)
                      }) {
                          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                      }
                  }
                  Text(
                      text = settingsPageTitle(targetPage),
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(start = if (targetPage == SettingsPage.ROOT) 0.dp else 8.dp)
                  )
              }
          }

          when (targetPage) {
              SettingsPage.ROOT -> {
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                      SettingsTile(
                          title = "Budget & Currency",
                          description = "Update your monthly limit and currency",
                          icon = Icons.Default.Payments,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              goTo(SettingsPage.BUDGET)
                          }
                      )
                      SettingsTile(
                          title = stringResource(R.string.settings_appearance),
                          description = stringResource(R.string.settings_appearance_desc),
                          icon = Icons.Default.DarkMode,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              goTo(SettingsPage.THEME)
                          }
                      )
                      SettingsSwitchTile(
                          title = stringResource(R.string.settings_reminders),
                          description = stringResource(R.string.settings_reminders_desc),
                          icon = Icons.Default.Notifications,
                          checked = reminderEnabled,
                          onCheckedChange = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              onReminderToggle(it)
                          }
                      )
                      SettingsInfoTile(
                          title = stringResource(R.string.settings_account),
                          description = accountEmail ?: stringResource(R.string.settings_account_unknown),
                          icon = Icons.Default.Person,
                      )
                      SettingsTile(
                          title = stringResource(R.string.settings_offline),
                          description = if (pendingCount > 0) {
                              "$pendingCount waiting to sync"
                          } else {
                              stringResource(R.string.settings_offline_none)
                          },
                          icon = Icons.Default.CloudSync,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              onSyncNow()
                          }
                      )
                      SettingsTile(
                          title = stringResource(R.string.settings_pin_shortcut),
                          description = stringResource(R.string.settings_pin_shortcut_desc),
                          icon = Icons.Default.PushPin,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              onPinShortcut()
                          }
                      )
                      SettingsTile(
                          title = "Account & Storage",
                          description = "Clear setup or logout from device",
                          icon = Icons.Default.Security,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              goTo(SettingsPage.DANGER_ZONE)
                          }
                      )
                  }
              }
              SettingsPage.BUDGET -> {
                  BudgetSettingsPage(
                      userSettings = userSettings,
                      supportedCurrencies = supportedCurrencies,
                      isLoading = isLoading,
                      isUpdating = isUpdating,
                      onUpdateSettings = onUpdateSettings
                  )
              }
              SettingsPage.THEME -> {
                  ThemeSettingsPage(
                      themeMode = themeMode,
                      onThemeChange = onThemeChange,
                  )
              }
              SettingsPage.DANGER_ZONE -> {
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                      SettingsTile(
                          title = stringResource(R.string.settings_clear_setup),
                          description = stringResource(R.string.settings_clear_setup_desc),
                          icon = Icons.Default.DeleteSweep,
                          onClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              onClearSetup()
                          },
                          isDestructive = true
                      )
                  }
              }
          }
      }
    }

    if (showCloseButton && currentPage == SettingsPage.ROOT) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClose()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.settings_back_to_add), style = MaterialTheme.typography.titleMedium)
        }
    }
  }
}

@Composable
fun ThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeOptionRow(
            label = stringResource(R.string.settings_theme_system),
            selected = themeMode == ThemeMode.SYSTEM,
            onSelect = { onThemeChange(ThemeMode.SYSTEM) },
        )
        ThemeOptionRow(
            label = stringResource(R.string.settings_theme_light),
            selected = themeMode == ThemeMode.LIGHT,
            onSelect = { onThemeChange(ThemeMode.LIGHT) },
        )
        ThemeOptionRow(
            label = stringResource(R.string.settings_theme_dark),
            selected = themeMode == ThemeMode.DARK,
            onSelect = { onThemeChange(ThemeMode.DARK) },
        )
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            RadioButton(selected = selected, onClick = onSelect)
        }
    }
}

@Composable
fun BudgetSettingsPage(
    userSettings: UserSettings?,
    supportedCurrencies: List<String>,
    isLoading: Boolean,
    isUpdating: Boolean,
    onUpdateSettings: (UserSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var monthlyBudget by remember(userSettings) { mutableStateOf(userSettings?.monthlyBudget?.toLong()?.toString() ?: "") }
    var budgetStartDay by remember(userSettings) { mutableStateOf(userSettings?.budgetStartDay?.toString() ?: "") }
    var selectedCurrency by remember(userSettings) { mutableStateOf(userSettings?.defaultCurrency ?: "IDR") }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching data...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (userSettings != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monthlyBudget,
                    onValueChange = {
                        if (it.length <= 9 && it.all { char -> char.isDigit() }) {
                            monthlyBudget = it
                        }
                    },
                    label = { Text("Monthly Budget") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = budgetStartDay,
                    onValueChange = {
                        if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                            budgetStartDay = it
                        }
                    },
                    label = { Text("Budget Start Day (1-31)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium
                )

                Box {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Default Currency") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { isCurrencyMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    )
                    DropdownMenu(
                        expanded = isCurrencyMenuExpanded,
                        onDismissRequest = { isCurrencyMenuExpanded = false }
                    ) {
                        supportedCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    selectedCurrency = currency
                                    isCurrencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        focusManager.clearFocus()
                        keyboardController?.hide()

                        val budget = monthlyBudget.toDoubleOrNull() ?: 0.0
                        val startDay = budgetStartDay.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        onUpdateSettings(UserSettings(budget, startDay, selectedCurrency))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTile(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  isDestructive: Boolean = false
) {
  val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
  val iconContainer = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

  Surface(
    onClick = onClick,
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    SettingsTileContent(title, description, icon, color, iconContainer, showChevron = true)
  }
}

@Composable
fun SettingsInfoTile(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    SettingsTileContent(
      title,
      description,
      icon,
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.primaryContainer,
      showChevron = false,
    )
  }
}

@Composable
fun SettingsSwitchTile(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SettingsIcon(icon, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
  }
}

@Composable
private fun SettingsTileContent(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  iconContainer: Color,
  showChevron: Boolean,
) {
  Row(
    modifier = Modifier.padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SettingsIcon(icon, color, iconContainer)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showChevron) {
      Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
      )
    }
  }
}

@Composable
private fun SettingsIcon(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  container: Color,
) {
  Surface(
    color = container,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.size(40.dp)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
    }
  }
}
