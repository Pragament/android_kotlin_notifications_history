package com.pragament.notificationshistory.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

enum class SnoozeOption(val label: String, val durationMs: Long) {
    FIFTEEN_MINUTES("15 minutes", 15 * 60 * 1000L),
    ONE_HOUR("1 hour", 60 * 60 * 1000L),
    THREE_HOURS("3 hours", 3 * 60 * 60 * 1000L),
    TOMORROW_MORNING("Tomorrow morning", calculateTomorrowMorning()),
    END_OF_DAY("End of day", calculateEndOfDay())
}

private fun calculateTomorrowMorning(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 9)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis - System.currentTimeMillis()
}

private fun calculateEndOfDay(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 18)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    // If it's already past 6 PM, set to tomorrow
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return calendar.timeInMillis - System.currentTimeMillis()
}

@Composable
fun SnoozeDialog(
    onDismiss: () -> Unit,
    onSnooze: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableLongStateOf(SnoozeOption.ONE_HOUR.durationMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Snooze Notification",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose when to be reminded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                SnoozeOption.entries.forEach { option ->
                    SnoozeOptionRow(
                        option = option,
                        selected = selectedOption == option.durationMs,
                        onClick = { selectedOption = option.durationMs }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val snoozeUntil = System.currentTimeMillis() + selectedOption
                    onSnooze(snoozeUntil)
                }
            ) {
                Text("Snooze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SnoozeOptionRow(
    option: SnoozeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
