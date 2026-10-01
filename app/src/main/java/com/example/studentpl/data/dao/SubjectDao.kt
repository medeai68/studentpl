package com.example.studentpl.data.dao

import androidx.room.*
import com.example.studentpl.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE userEmail = :userEmail ORDER BY name ASC")
    fun getAllSubjects(userEmail: String): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Int): Int

    @Query("DELETE FROM subjects WHERE userEmail = :userEmail")
    suspend fun clearSubjectsForUser(userEmail: String): Int
}
