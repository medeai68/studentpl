package com.example.studentpl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: String,
    val completed: Boolean,
    val userEmail: String = "",

    // ── Reminder settings (per-task) ─────────────────────────────────────
    val reminderEnabled: Boolean = true,
    // Comma-separated list of days-before-due to fire reminders
    // e.g. "0,1,2" = day-of, 1 day before, 2 days before
    val reminderDaysBefore: String = "0,1,2",
    // Time of day for reminders, HH:mm format (24h)
    val reminderTime: String = "09:00"
)
