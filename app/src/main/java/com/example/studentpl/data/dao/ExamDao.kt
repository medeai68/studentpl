package com.example.studentpl.data.dao

import androidx.room.*
import com.example.studentpl.model.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE userEmail = :userEmail ORDER BY examDate ASC")
    fun getAllExams(userEmail: String): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Int): Int

    @Query("DELETE FROM exams WHERE userEmail = :userEmail")
    suspend fun clearExamsForUser(userEmail: String): Int
}
