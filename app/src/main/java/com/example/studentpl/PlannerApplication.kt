package com.example.studentpl

import android.app.Application
import com.example.studentpl.data.AppDatabase
import com.example.studentpl.data.preferences.ThemePreferenceManager
import com.example.studentpl.network.RetrofitClient
import com.example.studentpl.repository.AiRepository
import com.example.studentpl.repository.PlannerRepository
import com.example.studentpl.util.NotificationScheduler

class PlannerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        PlannerRepository(
            database.subjectDao(),
            database.taskDao(),
            database.examDao(),
            database.userDao()
        )
    }
    val aiRepository by lazy {
        AiRepository(
            RetrofitClient.deepSeekApiService
        )
    }
    val notificationScheduler by lazy { NotificationScheduler(this) }
    val themePreferenceManager by lazy { ThemePreferenceManager(this) }
}
