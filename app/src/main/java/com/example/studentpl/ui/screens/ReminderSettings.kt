package com.example.studentpl.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Reusable reminder customization section used in Add/Edit screens.
 *
 * @param enabled      whether reminders are on
 * @param onToggle     called when the switch is toggled
 * @param daysBefore   comma-separated list of day offsets, e.g. "0,1,2"
 * @param onDaysChange called with the new comma-separated string
 * @param time         HH:mm 24h time string, e.g. "09:00"
 * @param onTimeChange called with the new HH:mm string
 * @param dayOptions   which day-offset options to show (tasks vs exams)
 */
@Composable
fun ReminderSettingsSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    daysBefore: String,
    onDaysChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    dayOptions: List<ReminderDayOption> = ReminderDayOption.taskDefaults
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row with toggle ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Reminders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (enabled) buildSummary(daysBefore, time, dayOptions)
                            else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle(it); if (it) expanded = true },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Expandable settings ───────────────────────────────────────
            AnimatedVisibility(
                visible = expanded && enabled,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                exit = shrinkVertically(tween(300)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // ── Day selection chips ──────────────────────────────
                    Text(
                        "Remind me...",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val selectedDays = parseDays(daysBefore)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayOptions.forEach { option ->
                            val isSelected = option.daysBefore in selectedDays
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val new = if (isSelected)
                                        selectedDays - option.daysBefore
                                    else
                                        selectedDays + option.daysBefore
                                    onDaysChange(
                                        new.sorted().joinToString(",") { it.toString() }
                                    )
                                },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Time picker ──────────────────────────────────────
                    Text(
                        "At what time?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    TimePickerButton(
                        currentTime = time,
                        onTimePicked = onTimeChange
                    )
                }
            }
        }
    }
}

// ── Time picker button ──────────────────────────────────────────────────────

@Composable
private fun TimePickerButton(currentTime: String, onTimePicked: (String) -> Unit) {
    val context = LocalContext.current
    val parts = currentTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, h, m ->
                    onTimePicked(String.format(Locale.getDefault(), "%02d:%02d", h, m))
                },
                hour, minute, true // is24HourView = true
            ).show()
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(formatTimeDisplay(currentTime), style = MaterialTheme.typography.bodyLarge)
    }
}

// ── Data class for day option ───────────────────────────────────────────────

data class ReminderDayOption(
    val daysBefore: Int,
    val label: String
) {
    companion object {
        val taskDefaults = listOf(
            ReminderDayOption(0, "Day of"),
            ReminderDayOption(1, "1 day before"),
            ReminderDayOption(2, "2 days before"),
            ReminderDayOption(3, "3 days before")
        )
        val examDefaults = listOf(
            ReminderDayOption(0, "Day of"),
            ReminderDayOption(1, "1 day before"),
            ReminderDayOption(3, "3 days before"),
            ReminderDayOption(7, "7 days before")
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun parseDays(raw: String): Set<Int> =
    raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

private fun buildSummary(daysBefore: String, time: String, options: List<ReminderDayOption>): String {
    val days = parseDays(daysBefore)
    val labelMap = options.associate { it.daysBefore to it.label }
    val labels = days.sorted().mapNotNull { labelMap[it] }
    return if (labels.isEmpty()) "No reminders set"
    else "${labels.joinToString(", ")} at ${formatTimeDisplay(time)}"
}

private fun formatTimeDisplay(hhmm: String): String {
    val parts = hhmm.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
