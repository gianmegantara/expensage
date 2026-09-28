package online.expensage.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import online.expensage.android.R

@Composable
fun SetupScreen(
  setupLinkInput: String,
  onSetupLinkInputChanged: (String) -> Unit,
  onImportSetup: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = stringResource(R.string.setup_title),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    QuickAddInputTile(
      value = setupLinkInput,
      onValueChange = onSetupLinkInputChanged,
      placeholder = stringResource(R.string.setup_placeholder),
      icon = Icons.Default.Link,
      singleLine = false,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Done
      ),
      keyboardActions = KeyboardActions(onDone = { 
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onImportSetup() 
      })
    )

    Button(
      onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onImportSetup()
      },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = MaterialTheme.shapes.medium,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
      ),
      enabled = setupLinkInput.isNotBlank()
    ) {
      Text(stringResource(R.string.setup_import_button), style = MaterialTheme.typography.titleMedium)
    }
  }
}
