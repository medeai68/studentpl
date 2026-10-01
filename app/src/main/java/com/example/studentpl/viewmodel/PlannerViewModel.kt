package com.example.studentpl.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.studentpl.data.DatabaseConnector
import com.example.studentpl.data.preferences.AppTheme
import com.example.studentpl.data.preferences.ThemePreferenceManager
import com.example.studentpl.model.Exam
import com.example.studentpl.model.Subject
import com.example.studentpl.model.Task
import com.example.studentpl.model.User
import com.example.studentpl.network.model.ChatMessage
import com.example.studentpl.repository.AiRepository
import com.example.studentpl.repository.PlannerRepository
import com.example.studentpl.util.NotificationScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val repository: PlannerRepository,
    private val aiRepository: AiRepository,
    private val notificationScheduler: NotificationScheduler,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "PlannerViewModel"
    }

    private val _loginStatus = MutableSharedFlow<String>()
    val loginStatus: SharedFlow<String> = _loginStatus

    private val _signupStatus = MutableSharedFlow<String>()
    val signupStatus: SharedFlow<String> = _signupStatus

    private val _syncStatus = MutableSharedFlow<String>()
    val syncStatus: SharedFlow<String> = _syncStatus

    // ── Helper ────────────────────────────────────────────────────────────
    private suspend fun requireUserEmail(): String? {
        val email = repository.user.firstOrNull()?.email
        if (email == null) Log.w(TAG, "requireUserEmail: no user logged in — operation skipped")
        return email
    }

    // ── Auth ──────────────────────────────────────────────────────────────
    fun login(email: String, password: String) = viewModelScope.launch {
        try {
            val trimmedEmail = email.trim()
            Log.d(TAG, "Attempting login for: $trimmedEmail")
            val (user, status) = DatabaseConnector.authenticate(trimmedEmail, password)
            if (user != null) {
                Log.d(TAG, "Login successful — fetching data from server...")

                // 1. Wipe leftover data from a previous session
                repository.clearAllUserData()

                // 2. Save the authenticated user locally
                repository.insertUser(user)
                Log.d(TAG, "User saved: ${user.email}")

                // 3. Pull this user's data from the remote MySQL server
                try {
                    val remoteTasks = DatabaseConnector.fetchTasks(user.email)
                    remoteTasks.forEach { repository.insertTask(it) }
                    Log.d(TAG, "Fetched ${remoteTasks.size} tasks")

                    val remoteExams = DatabaseConnector.fetchExams(user.email)
                    remoteExams.forEach { repository.insertExam(it) }
                    Log.d(TAG, "Fetched ${remoteExams.size} exams")

                    val remoteSubjects = DatabaseConnector.fetchSubjects(user.email)
                    remoteSubjects.forEach { repository.insertSubject(it) }
                    Log.d(TAG, "Fetched ${remoteSubjects.size} subjects")
                } catch (fetchErr: Throwable) {
                    Log.w(TAG, "Server fetch partially failed (offline?): ${fetchErr.message}")
                }

                _loginStatus.emit("SUCCESS")
            } else {
                Log.d(TAG, "Login failed: $status")
                _loginStatus.emit(status)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error during login", e)
            _loginStatus.emit("DATABASE_ERROR")
        }
    }

    fun signup(email: String, password: String) = viewModelScope.launch {
        try {
            val trimmedEmail = email.trim()
            Log.d(TAG, "Attempting signup for: $trimmedEmail")

            if (trimmedEmail.isEmpty() || password.isEmpty()) {
                _signupStatus.emit("FIELDS_EMPTY")
                return@launch
            }

            val status = DatabaseConnector.register(trimmedEmail, password)
            if (status == "SUCCESS") {
                Log.d(TAG, "Signup successful")
                _signupStatus.emit("SUCCESS")
            } else {
                Log.d(TAG, "Signup failed: $status")
                _signupStatus.emit(status)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error during signup", e)
            _signupStatus.emit("DATABASE_ERROR")
        }
    }

    fun logout() = viewModelScope.launch {
        Log.d(TAG, "Logging out — clearing all local data")
        repository.clearAllUserData()
    }

    // ── Theme ─────────────────────────────────────────────────────────────
    val appTheme: StateFlow<AppTheme> = themePreferenceManager.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        themePreferenceManager.setTheme(theme)
    }

    // ── AI Chat ───────────────────────────────────────────────────────────
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading

    fun askAi(prompt: String) = viewModelScope.launch {
        if (prompt.isBlank()) return@launch

        val userMessage = ChatMessage(role = "user", content = prompt)

        if (_chatMessages.value.isEmpty()) {
            val subjects = repository.allSubjects.firstOrNull() ?: emptyList()
            val context = "Subjects: ${subjects.joinToString { it.name }}"
            _chatMessages.value = listOf(aiRepository.getSystemPrompt(context))
        }

        _chatMessages.value = _chatMessages.value + userMessage
        _isAiLoading.value = true

        val result = aiRepository.getChatCompletion(_chatMessages.value)

        result.onSuccess { aiMessage ->
            _chatMessages.value = _chatMessages.value + aiMessage
        }.onFailure { e ->
            _chatMessages.value = _chatMessages.value + ChatMessage(
                role = "assistant",
                content = "Error: ${e.message ?: "Unknown error occurred"}"
            )
        }

        _isAiLoading.value = false
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    // ── Reactive data (auto-scoped to current user via repository) ────────
    val allSubjects: LiveData<List<Subject>> = repository.allSubjects.asLiveData()
    val allTasks: LiveData<List<Task>> = repository.allTasks.asLiveData()
    val allExams: LiveData<List<Exam>> = repository.allExams.asLiveData()
    val user: LiveData<User?> = repository.user.asLiveData()

    // ── Subject CRUD ─────────────────────────────────────────────────────
    fun insertSubject(subject: Subject) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = subject.copy(userEmail = email)
        val id = repository.insertSubject(scoped)
        Log.d(TAG, "Inserted subject id=$id name=${subject.name}")
        try {
            val ok = DatabaseConnector.syncSubject(scoped.copy(id = id.toInt()), email)
            if (!ok) _syncStatus.emit("Saved locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Saved locally — server unreachable")
        }
    }
    fun updateSubject(subject: Subject) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = subject.copy(userEmail = email)
        repository.updateSubject(scoped)
        Log.d(TAG, "Updated subject id=${subject.id}")
        try {
            val ok = DatabaseConnector.syncSubject(scoped, email)
            if (!ok) _syncStatus.emit("Updated locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Updated locally — server unreachable")
        }
    }
    fun deleteSubject(subject: Subject) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val deleted = repository.deleteSubject(subject)
        Log.d(TAG, "Deleted subject id=${subject.id} localRows=$deleted")
        if (deleted == 0) {
            Log.w(TAG, "Local delete affected 0 rows for subject id=${subject.id}")
            return@launch
        }
        try {
            val ok = DatabaseConnector.deleteSubject(subject.id, email)
            if (!ok) _syncStatus.emit("Removed locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Removed locally — server unreachable")
        }
    }

    // ── Task CRUD ────────────────────────────────────────────────────────
    fun getTaskById(id: Int, onResult: (Task?) -> Unit) = viewModelScope.launch {
        val email = requireUserEmail() ?: run { onResult(null); return@launch }
        onResult(repository.getTaskById(id, email))
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = task.copy(userEmail = email)
        val id = repository.insertTask(scoped)
        Log.d(TAG, "Inserted task id=$id title=${task.title}")
        try {
            val ok = DatabaseConnector.syncTask(scoped.copy(id = id.toInt()), email)
            if (!ok) _syncStatus.emit("Saved locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Saved locally — server unreachable")
        }
        notificationScheduler.scheduleTaskReminders(scoped.copy(id = id.toInt()))
    }
    fun updateTask(task: Task) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = task.copy(userEmail = email)
        repository.updateTask(scoped)
        Log.d(TAG, "Updated task id=${task.id}")
        try {
            val ok = DatabaseConnector.syncTask(scoped, email)
            if (!ok) _syncStatus.emit("Updated locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Updated locally — server unreachable")
        }
        if (scoped.completed) notificationScheduler.cancelReminder(scoped.id)
        else notificationScheduler.scheduleTaskReminders(scoped)
    }
    fun deleteTask(task: Task) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val deleted = repository.deleteTask(task)
        Log.d(TAG, "Deleted task id=${task.id} localRows=$deleted")
        if (deleted == 0) {
            Log.w(TAG, "Local delete affected 0 rows for task id=${task.id}")
            return@launch
        }
        try {
            val ok = DatabaseConnector.deleteTask(task.id, email)
            if (!ok) _syncStatus.emit("Removed locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Removed locally — server unreachable")
        }
        notificationScheduler.cancelTaskReminders(task.id)
    }

    // ── Exam CRUD ────────────────────────────────────────────────────────
    fun insertExam(exam: Exam) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = exam.copy(userEmail = email)
        val id = repository.insertExam(scoped)
        Log.d(TAG, "Inserted exam id=$id subject=${exam.subject}")
        try {
            val ok = DatabaseConnector.syncExam(scoped.copy(id = id.toInt()), email)
            if (!ok) _syncStatus.emit("Saved locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Saved locally — server unreachable")
        }
        notificationScheduler.scheduleExamReminders(scoped.copy(id = id.toInt()))
    }
    fun updateExam(exam: Exam) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val scoped = exam.copy(userEmail = email)
        repository.updateExam(scoped)
        Log.d(TAG, "Updated exam id=${exam.id}")
        try {
            val ok = DatabaseConnector.syncExam(scoped, email)
            if (!ok) _syncStatus.emit("Updated locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Updated locally — server unreachable")
        }
        notificationScheduler.scheduleExamReminders(scoped)
    }
    fun deleteExam(exam: Exam) = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        val deleted = repository.deleteExam(exam)
        Log.d(TAG, "Deleted exam id=${exam.id} subject=${exam.subject} localRows=$deleted")
        if (deleted == 0) {
            Log.w(TAG, "Local delete affected 0 rows for exam id=${exam.id} — nothing to delete")
            return@launch
        }
        try {
            val ok = DatabaseConnector.deleteExam(exam.id, email)
            if (!ok) _syncStatus.emit("Removed locally — server sync failed")
        } catch (_: Exception) {
            _syncStatus.emit("Removed locally — server unreachable")
        }
        notificationScheduler.cancelExamReminders(exam.id)
    }

    // ── Cloud Sync ────────────────────────────────────────────────────────
    fun syncAll() = viewModelScope.launch {
        val email = requireUserEmail() ?: return@launch
        _syncStatus.emit("Syncing with server...")

        var allSuccess = true

        repository.allSubjects.firstOrNull()?.forEach {
            if (!DatabaseConnector.syncSubject(it, email)) allSuccess = false
        }
        repository.allTasks.firstOrNull()?.forEach {
            if (!DatabaseConnector.syncTask(it, email)) allSuccess = false
        }
        repository.allExams.firstOrNull()?.forEach {
            if (!DatabaseConnector.syncExam(it, email)) allSuccess = false
        }

        if (allSuccess) _syncStatus.emit("All data synced successfully!")
        else _syncStatus.emit("Some items failed to sync. Please try again later.")
    }
}

class PlannerViewModelFactory(
    private val repository: PlannerRepository,
    private val aiRepository: AiRepository,
    private val notificationScheduler: NotificationScheduler,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlannerViewModel(
                repository, aiRepository, notificationScheduler, themePreferenceManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
