package com.example.studentpl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val examDate: String,
    val notes: String,
    val userEmail: String = "",

    // ── Reminder settings (per-exam) ─────────────────────────────────────
    val reminderEnabled: Boolean = true,
    // Comma-separated list of days-before-exam to fire reminders
    // e.g. "0,1,3,7" = day-of, 1 day, 3 days, 7 days before
    val reminderDaysBefore: String = "0,1,3,7",
    // Time of day for reminders, HH:mm format (24h)
    val reminderTime: String = "08:00"
)
