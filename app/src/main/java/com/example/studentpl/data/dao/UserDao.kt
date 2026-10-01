package com.example.studentpl.data.dao

import androidx.room.*
import com.example.studentpl.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM simoapp LIMIT 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM simoapp")
    suspend fun clearUser()

    // Clear all user-scoped data when switching accounts
    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("DELETE FROM exams")
    suspend fun clearAllExams()

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()
}
