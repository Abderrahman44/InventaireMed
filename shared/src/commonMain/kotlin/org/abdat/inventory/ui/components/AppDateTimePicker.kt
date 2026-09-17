package org.abdat.inventory.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
 * Interactive Date and Time selector field.
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
    var showTimeDialog by remember { mutableStateOf(false) }

    val displayValue = remember(dateValue, timeValue) {
        when {
            dateValue.isNotBlank() && timeValue.isNotBlank() -> "$dateValue $timeValue"
            dateValue.isNotBlank() -> dateValue
            else -> ""
        }
    }

    Box(modifier = modifier) {
        AppDatePickerField(
            value = dateValue,
            onValueChange = { newDate ->
                onDateTimeChange(newDate, timeValue.ifBlank { "08:00" })
            },
            label = label,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }

    if (showTimeDialog) {
        val initialHour = timeValue.split(":").firstOrNull()?.toIntOrNull() ?: 12
        val initialMinute = timeValue.split(":").getOrNull(1)?.toIntOrNull() ?: 0

        AppTimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onConfirm = { h, m ->
                val formattedTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                onDateTimeChange(dateValue, formattedTime)
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}
