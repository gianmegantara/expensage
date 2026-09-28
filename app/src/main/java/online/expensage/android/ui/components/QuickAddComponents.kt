package online.expensage.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import online.expensage.android.R
import online.expensage.android.ui.SuccessState
import java.util.Calendar

@Composable
fun QuickAddScreen(
  note: String,
  amount: String,
  isSubmitting: Boolean,
  success: SuccessState?,
  onNoteChanged: (String) -> Unit,
  onAmountChanged: (String) -> Unit,
  onSubmit: () -> Unit,
  onDismissSuccess: () -> Unit,
  onOpenReport: (String) -> Unit,
  onRequestReport: () -> Unit,
  isOnline: Boolean,
  isSyncing: Boolean = false,
  isFetchingReport: Boolean = false,
) {
  val focusManager = LocalFocusManager.current
  val haptic = LocalHapticFeedback.current
  val focusRequester = remember { FocusRequester() }

  // Greeting logic cached at composition start
  val greeting = remember {
    val calendar = Calendar.getInstance()
    when (calendar.get(Calendar.HOUR_OF_DAY)) {
      in 5..11 -> R.string.greeting_morning
      in 12..16 -> R.string.greeting_afternoon
      in 17..20 -> R.string.greeting_evening
      else -> R.string.greeting_night
    }
  }

  // High-performance focus handling
  LaunchedEffect(success) {
    if (success != null) {
      focusManager.clearFocus()
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  Box(contentAlignment = Alignment.Center) {
    AnimatedVisibility(
      visible = success == null,
      enter = fadeIn() + scaleIn(initialScale = 1.1f),
      exit = fadeOut() + scaleOut(targetScale = 1.1f)
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Branding and Greeting Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              stringResource(greeting),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              stringResource(R.string.greeting_prompt),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          // Added Sync Status Indicator
          androidx.compose.animation.AnimatedVisibility(visible = isSyncing) {
             CircularProgressIndicator(
               modifier = Modifier.size(24.dp).padding(start = 8.dp),
               color = MaterialTheme.colorScheme.primary,
               strokeWidth = 2.dp
             )
          }
        }

        QuickAddInputTile(
          value = note,
          onValueChange = { if (it.length <= 50) onNoteChanged(it) },
          placeholder = stringResource(R.string.quick_add_note_placeholder),
          icon = Icons.Default.Edit,
          modifier = Modifier.focusRequester(focusRequester),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
          ),
          keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        QuickAddInputTile(
          value = amount,
          onValueChange = { input ->
            if (input.length <= 9 && input.all { it.isDigit() || it == '.' || it == ',' }) {
              onAmountChanged(input)
            }
          },
          placeholder = stringResource(R.string.quick_add_amount_placeholder),
          icon = Icons.Default.Payments,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(onDone = { 
            focusManager.clearFocus()
            onSubmit() 
          }),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isOnline) {
            OutlinedButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRequestReport()
              },
              modifier = Modifier.size(56.dp),
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              contentPadding = PaddingValues(0.dp),
              enabled = !isFetchingReport
            ) {
              if (isFetchingReport) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(
                  Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = stringResource(R.string.quick_add_view_report),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Button(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              focusManager.clearFocus()
              onSubmit()
            },
            modifier = Modifier.weight(1f).height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
              disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            ),
            enabled = !isSubmitting && amount.isNotBlank()
          ) {
            if (isSubmitting) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                if (!isOnline) {
                  Spacer(Modifier.width(12.dp))
                  Text(stringResource(R.string.quick_add_saving_offline), style = MaterialTheme.typography.titleSmall)
                }
              }
            } else {
              Text(stringResource(R.string.quick_add_save_button), style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
    }

    AnimatedVisibility(
      visible = success != null,
      enter = fadeIn() + scaleIn(initialScale = 0.8f),
      exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
      if (success != null) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.padding(vertical = 20.dp)
        ) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              success.note,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              success.displayAmount,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary
            )
            if (success.isOffline) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Text(
                  stringResource(R.string.quick_add_saved_offline),
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
          
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(modifier = Modifier.height(56.dp).fillMaxWidth()) {
              if (isOnline && success.reportUrl != null) {
                Button(
                  onClick = { onOpenReport(success.reportUrl) },
                  modifier = Modifier.fillMaxSize(),
                  shape = MaterialTheme.shapes.medium,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(stringResource(R.string.quick_add_view_report), style = MaterialTheme.typography.titleMedium)
                }
              }
            }

            Button(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismissSuccess()
              },
              modifier = Modifier.fillMaxWidth().height(56.dp),
              shape = MaterialTheme.shapes.medium,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Text(stringResource(R.string.quick_add_new_button), style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
fun QuickAddInputTile(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  prefix: @Composable (() -> Unit)? = null,
  singleLine: Boolean = true,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
      
      Spacer(modifier = Modifier.width(12.dp))

      TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
        modifier = Modifier.weight(1f),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          disabledContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        prefix = prefix,
        singleLine = singleLine
      )
    }
  }
}
