package com.example.studentpl.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.studentpl.PlannerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_SUMMARY = "com.example.studentpl.DAILY_SUMMARY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // After a reboot, re-register all alarms
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllAlarms(context)
            return
        }

        // Smart daily summary — builds a dynamic message from the database
        if (intent.action == ACTION_DAILY_SUMMARY) {
            handleDailySummary(context)
            return
        }

        // Standard task/exam reminder — just show the pre-built message
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: "You have an upcoming item."
        val notificationId = intent.getIntExtraCompat("notificationId", 0)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(title, message, notificationId)
    }

    // ── Smart daily summary ──────────────────────────────────────────────

    private fun handleDailySummary(context: Context) {
        val app = context.applicationContext as PlannerApplication
        val repository = app.repository
        val helper = NotificationHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                val user = repository.user.first()
                if (user == null) {
                    // No user logged in — skip
                    return@launch
                }

                val allTasks = repository.allTasks.first()
                val allExams = repository.allExams.first()

                // Tasks due tomorrow
                val tomorrowTasks = allTasks.filter {
                    !it.completed && parseDate(it.dueDate) == tomorrow
                }

                // Exams in the next 3 days
                val upcomingExams = allExams.filter {
                    val d = parseDate(it.examDate) ?: return@filter false
                    !d.isBefore(today) && !d.isAfter(today.plusDays(3))
                }

                // Overdue tasks (due before today, not completed)
                val overdueTasks = allTasks.filter {
                    !it.completed && parseDate(it.dueDate)?.isBefore(today) == true
                }

                // Build the message
                val parts = mutableListOf<String>()

                if (tomorrowTasks.isNotEmpty()) {
                    parts.add("📋 Tomorrow: ${tomorrowTasks.joinToString { it.title }}")
                }
                if (upcomingExams.isNotEmpty()) {
                    val examText = upcomingExams.joinToString { "${it.subject} (${it.examDate})" }
                    parts.add("📚 Upcoming exams: $examText")
                }
                if (overdueTasks.isNotEmpty()) {
                    parts.add("⚠️ ${overdueTasks.size} overdue task(s) need attention")
                }

                val title = "📊 Daily Study Summary"
                val message = if (parts.isEmpty()) {
                    "All caught up! Take some time to review or plan ahead. 🌟"
                } else {
                    parts.joinToString("\n")
                }

                helper.showNotification(title, message, 999_999)
            } catch (_: Exception) {
                // If anything fails (e.g., database not ready), show a generic reminder
                helper.showNotification(
                    "📊 Daily Planner Review",
                    "Time to check your tasks and exams for the coming days!",
                    999_999
                )
            }
        }
    }

    // ── Boot reschedule ──────────────────────────────────────────────────

    private fun rescheduleAllAlarms(context: Context) {
        val app = context.applicationContext as PlannerApplication
        val repository = app.repository
        val scheduler = app.notificationScheduler

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = repository.allTasks.first()
                tasks.forEach { task ->
                    scheduler.scheduleTaskReminders(task)
                }

                val exams = repository.allExams.first()
                exams.forEach { exam ->
                    scheduler.scheduleExamReminders(exam)
                }

                scheduler.scheduleDailySummary()
            } catch (_: Exception) {
                // If reschedule fails, the daily summary will still fire
                scheduler.scheduleDailySummary()
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun parseDate(raw: String): LocalDate? =
        try { LocalDate.parse(raw) } catch (_: Exception) { null }

    private fun Intent.getIntExtraCompat(key: String, defaultValue: Int): Int =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getIntExtra(key, defaultValue)
        } else {
            @Suppress("DEPRECATION") getIntExtra(key, defaultValue)
        }
}
