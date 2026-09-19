package org.abdat.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.abdat.inventory.theme.AppTheme

/**
 * Reusable Material 3 interactive DatePicker field for desktop forms.
 * Replaces manual text entry with an interactive calendar dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Date",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "AAAA-MM-JJ"
) {
    var showDialog by remember { mutableStateOf(false) }

    val initialMillis = remember(value) {
        try {
            if (value.isNotBlank()) {
                val cleanDate = value.trim().substringBefore("T").substringBefore(" ")
                Instant.parse("${cleanDate}T00:00:00Z").toEpochMilliseconds()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = { if (enabled) showDialog = true }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Sélectionner la date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showDialog = true },
            enabled = enabled,
            singleLine = true
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            onValueChange(date.toString())
                        }
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Reusable Material 3 interactive TimePicker dialog for desktop forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int = 12,
    initialMinute: Int = 0,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

/**
 * Interactive Date and Time selector field designed for desktop forms.
 * Provides a compact input with sequential DatePicker and TimePicker dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "AAAA-MM-JJ HH:mm",
    timeDialogTitle: String = "Sélectionner l'heure",
    height: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(6.dp),
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf("") }

    val initialMillis = remember(value) {
        try {
            if (value.isNotBlank()) {
                val cleanDate = value.trim().substringBefore("T").substringBefore(" ")
                Instant.parse("${cleanDate}T00:00:00Z").toEpochMilliseconds()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(AppTheme.colors.surfaceWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clickable(enabled = enabled) { showDatePicker = true },
            tint = if (enabled) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.size(24.dp),
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Sélectionner la date et l'heure",
                modifier = Modifier.size(16.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary.copy(alpha = 0.5f)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date.toString()
                            tempDate = selectedDate
                            showDatePicker = false
                            showTimePicker = true
                        } ?: run {
                            showDatePicker = false
                        }
                    }
                ) {
                    Text("Suivant (Heure)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val existingTime = value.substringAfter(" ", "12:00")
        val initH = existingTime.split(":").firstOrNull()?.toIntOrNull() ?: 12
        val initM = existingTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initH,
            initialMinute = initM,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = timePickerState.hour.toString().padStart(2, '0')
                        val m = timePickerState.minute.toString().padStart(2, '0')
                        val datePart = tempDate.ifBlank { value.substringBefore(" ") }
                        onValueChange("$datePart $h:$m")
                        showTimePicker = false
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Annuler")
                }
            },
            title = { Text(timeDialogTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

/**
 * Backward compatible overload accepting separate date and time values.
 */
@Composable
fun AppDateTimePickerField(
    dateValue: String,
    timeValue: String,
    onDateTimeChange: (date: String, time: String) -> Unit,
    label: String = "Date & Heure",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AppDateTimePickerField(
        value = if (dateValue.isNotBlank() && timeValue.isNotBlank()) "$dateValue $timeValue" else dateValue,
        onValueChange = { fullStr ->
            val d = fullStr.substringBefore(" ")
            val t = fullStr.substringAfter(" ", "12:00")
            onDateTimeChange(d, t)
        },
        modifier = modifier,
        enabled = enabled
    )
}
