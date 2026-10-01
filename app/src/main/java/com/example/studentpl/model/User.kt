package com.example.studentpl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents the application user.
 * @param id Unique identifier
 * @param email Contact email address
 */
@Serializable
@Entity(tableName = "simoapp")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val passwordHash: String = "" // In a real app, this should be handled securely
)
