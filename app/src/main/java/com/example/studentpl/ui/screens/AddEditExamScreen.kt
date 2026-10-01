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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentpl.model.Exam
import com.example.studentpl.viewmodel.PlannerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExamScreen(viewModel: PlannerViewModel, examId: Int, onBack: () -> Unit) {
    val exams by viewModel.allExams.observeAsState(initial = emptyList())
    var subject by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // ── Reminder state ────────────────────────────────────────────────────
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderDaysBefore by remember { mutableStateOf("0,1,3,7") }
    var reminderTime by remember { mutableStateOf("08:00") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(examId, exams) {
        if (examId != 0) {
            exams.find { it.id == examId }?.let {
                subject = it.subject; examDate = it.examDate; notes = it.notes
                reminderEnabled = it.reminderEnabled
                reminderDaysBefore = it.reminderDaysBefore
                reminderTime = it.reminderTime
            }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> examDate = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d) },
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
                title = { Text(if (examId == 0) "New Exam" else "Edit Exam", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
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
                value = subject, onValueChange = { subject = it },
                label = { Text("Subject Name") },
                leadingIcon = { Icon(Icons.Default.School, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = fieldShape, colors = fieldColors
            )

            OutlinedTextField(
                value = examDate, onValueChange = {},
                label = { Text("Exam Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, "Pick") } },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true, shape = fieldShape, colors = fieldColors, enabled = false
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
                Text(if (examDate.isEmpty()) "Select Exam Date" else examDate)
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes / Topics to Study") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4, shape = fieldShape, colors = fieldColors
            )

            // ── Reminder Settings ─────────────────────────────────────────
            ReminderSettingsSection(
                enabled = reminderEnabled,
                onToggle = { reminderEnabled = it },
                daysBefore = reminderDaysBefore,
                onDaysChange = { reminderDaysBefore = it },
                time = reminderTime,
                onTimeChange = { reminderTime = it },
                dayOptions = ReminderDayOption.examDefaults
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val exam = Exam(
                        id = if (examId == 0) 0 else examId,
                        subject = subject, examDate = examDate, notes = notes,
                        reminderEnabled = reminderEnabled,
                        reminderDaysBefore = reminderDaysBefore,
                        reminderTime = reminderTime
                    )
                    if (examId == 0) viewModel.insertExam(exam) else viewModel.updateExam(exam)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = subject.isNotBlank() && examDate.isNotBlank()
            ) {
                Icon(if (examId == 0) Icons.Default.Add else Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (examId == 0) "Create Exam" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
