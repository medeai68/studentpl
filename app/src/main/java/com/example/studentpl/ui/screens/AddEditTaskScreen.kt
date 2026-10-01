package com.example.studentpl.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentpl.model.Task
import com.example.studentpl.viewmodel.PlannerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(viewModel: PlannerViewModel, taskId: Int, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }

    // ── Reminder state ────────────────────────────────────────────────────
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderDaysBefore by remember { mutableStateOf("0,1,2") }
    var reminderTime by remember { mutableStateOf("09:00") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(taskId) {
        if (taskId != 0) {
            viewModel.getTaskById(taskId) { task ->
                task?.let {
                    title = it.title; description = it.description
                    dueDate = it.dueDate; isCompleted = it.completed
                    reminderEnabled = it.reminderEnabled
                    reminderDaysBefore = it.reminderDaysBefore
                    reminderTime = it.reminderTime
                }
            }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> dueDate = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val fieldShape = RoundedCornerShape(14.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == 0) "New Task" else "Edit Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Task Title") },
                leadingIcon = { Icon(Icons.Default.Task, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = fieldShape, colors = fieldColors
            )

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, shape = fieldShape, colors = fieldColors
            )

            OutlinedTextField(
                value = dueDate, onValueChange = {},
                label = { Text("Due Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.EditCalendar, "Pick Date")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true, shape = fieldShape, colors = fieldColors,
                enabled = false
            )

            Button(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (dueDate.isEmpty()) "Select Date" else dueDate)
            }

            if (taskId != 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isCompleted, onCheckedChange = { isCompleted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as completed", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // ── Reminder Settings ─────────────────────────────────────────
            ReminderSettingsSection(
                enabled = reminderEnabled,
                onToggle = { reminderEnabled = it },
                daysBefore = reminderDaysBefore,
                onDaysChange = { reminderDaysBefore = it },
                time = reminderTime,
                onTimeChange = { reminderTime = it },
                dayOptions = ReminderDayOption.taskDefaults
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val task = Task(
                        id = if (taskId == 0) 0 else taskId,
                        title = title, description = description,
                        dueDate = dueDate, completed = isCompleted,
                        reminderEnabled = reminderEnabled,
                        reminderDaysBefore = reminderDaysBefore,
                        reminderTime = reminderTime
                    )
                    if (taskId == 0) viewModel.insertTask(task) else viewModel.updateTask(task)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Icon(if (taskId == 0) Icons.Default.Add else Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (taskId == 0) "Create Task" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
