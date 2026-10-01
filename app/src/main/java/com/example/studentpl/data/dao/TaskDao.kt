package com.example.studentpl.data.dao

import androidx.room.*
import com.example.studentpl.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userEmail = :userEmail ORDER BY dueDate ASC")
    fun getAllTasks(userEmail: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id AND userEmail = :userEmail")
    suspend fun getTaskById(id: Int, userEmail: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int): Int

    @Query("DELETE FROM tasks WHERE userEmail = :userEmail")
    suspend fun clearTasksForUser(userEmail: String): Int
}
