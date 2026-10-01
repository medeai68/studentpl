package com.example.studentpl.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.studentpl.model.Exam
import com.example.studentpl.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Smart per-item notification scheduler.
 *
 * Each Task/Exam carries its own reminder settings:
 *   - reminderEnabled: Boolean          toggle on/off
 *   - reminderDaysBefore: String        comma-separated day offsets, e.g. "0,1,2"
 *   - reminderTime: String              HH:mm 24h time, e.g. "09:00"
 *
 * Notification ID scheme:
 *   Task at offset D:  task.id + (D * 10_000)
 *   Exam at offset D:  exam.id + 50_000 + (D * 10_000)
 *   Daily summary:     999_999
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ── Public API ────────────────────────────────────────────────────────

    /** Schedule all notification stages for a task using its own settings. */
    fun scheduleTaskReminders(task: Task) {
        cancelTaskReminders(task.id)

        if (!task.reminderEnabled || task.completed) return

        val dueDate = parseDate(task.dueDate) ?: return
        val offsets = parseDayOffsets(task.reminderDaysBefore)
        val time = parseTime(task.reminderTime) ?: LocalTime.of(9, 0)

        val baseId = task.id
        val label = "\"${task.title}\""

        for (daysBefore in offsets.sortedDescending()) {
            val triggerDate = dueDate.minusDays(daysBefore.toLong())
            val (title, message) = taskMessageFor(label, daysBefore)
            schedule(triggerDate, time, title, message, baseId + (daysBefore * 10_000))
        }
    }

    /** Schedule all notification stages for an exam using its own settings. */
    fun scheduleExamReminders(exam: Exam) {
        cancelExamReminders(exam.id)

        if (!exam.reminderEnabled) return

        val examDate = parseDate(exam.examDate) ?: return
        val offsets = parseDayOffsets(exam.reminderDaysBefore)
        val time = parseTime(exam.reminderTime) ?: LocalTime.of(8, 0)

        val baseId = exam.id + 50_000
        val label = exam.subject

        for (daysBefore in offsets.sortedDescending()) {
            val triggerDate = examDate.minusDays(daysBefore.toLong())
            val (title, message) = examMessageFor(label, daysBefore)
            schedule(triggerDate, time, title, message, baseId + (daysBefore * 10_000))
        }
    }

    /** Cancel all notification stages for a task. */
    fun cancelTaskReminders(taskId: Int) {
        // Cancel all possible offset IDs (0 through 14 should cover any reasonable day range)
        for (d in 0..14) cancelAlarm(taskId + (d * 10_000))
    }

    /** Cancel all notification stages for an exam. */
    fun cancelExamReminders(examId: Int) {
        val base = examId + 50_000
        for (d in 0..14) cancelAlarm(base + (d * 10_000))
    }

    /** Legacy single-ID cancel (still used in some paths). */
    fun cancelReminder(notificationId: Int) = cancelAlarm(notificationId)

    /** 8:00 PM daily smart summary. */
    fun scheduleDailySummary() {
        val triggerTime = LocalDateTime.now()
            .withHour(20).withMinute(0).withSecond(0).withNano(0)
            .let { if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it }

        val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_SUMMARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 999_999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, triggerAtMillis,
            AlarmManager.INTERVAL_DAY, pendingIntent
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun schedule(
        date: LocalDate, time: LocalTime,
        title: String, message: String, notificationId: Int
    ) {
        val triggerAtMillis = LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("notificationId", notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )
    }

    private fun cancelAlarm(notificationId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ── Message builders ──────────────────────────────────────────────────

    private fun taskMessageFor(label: String, daysBefore: Int): Pair<String, String> = when (daysBefore) {
        0 -> "🔔 Task Due Today" to "$label is due today. Don't forget!"
        1 -> "📋 Task Due Tomorrow" to "$label is due tomorrow. Finish it up!"
        2 -> "⏰ Upcoming Task" to "$label is due in 2 days. Plan ahead!"
        else -> "📌 Task Reminder" to "$label is due in $daysBefore days."
    }

    private fun examMessageFor(subject: String, daysBefore: Int): Pair<String, String> = when (daysBefore) {
        0 -> "🎯 Exam Today!" to "Your $subject exam is today. Stay calm and do your best!"
        1 -> "📚 Exam Tomorrow!" to "Your $subject exam is tomorrow. Final review!"
        2 -> "📖 Exam in 2 Days" to "Your $subject exam is in 2 days. Keep studying!"
        3 -> "⏳ Exam in 3 Days" to "Your $subject exam is in 3 days. Ramp up your prep!"
        7 -> "📅 Exam in a Week" to "Your $subject exam is in 7 days. Start a study plan!"
        else -> "📌 Exam Reminder" to "Your $subject exam is in $daysBefore days."
    }

    // ── Parsing helpers ──────────────────────────────────────────────────

    private fun parseDate(raw: String): LocalDate? =
        try { LocalDate.parse(raw) } catch (_: Exception) { null }

    private fun parseTime(raw: String): LocalTime? =
        try { LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")) }
        catch (_: Exception) { null }

    private fun parseDayOffsets(raw: String): List<Int> =
        raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..14 }
            .ifEmpty { listOf(0) }
}
