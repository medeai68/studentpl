package com.example.studentpl.repository

import com.example.studentpl.data.dao.ExamDao
import com.example.studentpl.data.dao.SubjectDao
import com.example.studentpl.data.dao.TaskDao
import com.example.studentpl.data.dao.UserDao
import com.example.studentpl.model.Exam
import com.example.studentpl.model.Subject
import com.example.studentpl.model.Task
import com.example.studentpl.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class PlannerRepository(
    private val subjectDao: SubjectDao,
    private val taskDao: TaskDao,
    private val examDao: ExamDao,
    private val userDao: UserDao
) {
    // ── User ──────────────────────────────────────────────────────────────
    val user: Flow<User?> = userDao.getUser()
    suspend fun insertUser(user: User) = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun clearUser() = userDao.clearUser()

    suspend fun clearAllUserData() {
        userDao.clearUser()
        userDao.clearAllTasks()
        userDao.clearAllExams()
        userDao.clearAllSubjects()
    }

    // ── Subjects (scoped to current user) ─────────────────────────────────
    val allSubjects: Flow<List<Subject>> = user.flatMapLatest { currentUser ->
        if (currentUser != null) subjectDao.getAllSubjects(currentUser.email)
        else flowOf(emptyList())
    }
    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject): Int = subjectDao.deleteSubjectById(subject.id)

    // ── Tasks (scoped to current user) ────────────────────────────────────
    val allTasks: Flow<List<Task>> = user.flatMapLatest { currentUser ->
        if (currentUser != null) taskDao.getAllTasks(currentUser.email)
        else flowOf(emptyList())
    }
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task): Int = taskDao.deleteTaskById(task.id)
    suspend fun getTaskById(id: Int, userEmail: String) = taskDao.getTaskById(id, userEmail)

    // ── Exams (scoped to current user) ────────────────────────────────────
    val allExams: Flow<List<Exam>> = user.flatMapLatest { currentUser ->
        if (currentUser != null) examDao.getAllExams(currentUser.email)
        else flowOf(emptyList())
    }
    suspend fun insertExam(exam: Exam): Long = examDao.insertExam(exam)
    suspend fun updateExam(exam: Exam) = examDao.updateExam(exam)
    suspend fun deleteExam(exam: Exam): Int = examDao.deleteExamById(exam.id)
}
