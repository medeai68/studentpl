package com.example.studentpl.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector = icon) {
    object Login : Screen("login", "Login", Icons.Outlined.Person, Icons.Filled.Person)
    object Signup : Screen("signup", "Signup", Icons.Outlined.Person, Icons.Filled.Person)
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle)
    object Exams : Screen("exams", "Exams", Icons.Outlined.School, Icons.Filled.School)
    object Profile : Screen("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    object Subjects : Screen("subjects", "Subjects", Icons.Outlined.MenuBook, Icons.Filled.MenuBook)
    object AiAssistant : Screen("ai_assistant", "AI", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome)
    object AddEditSubject : Screen("add_edit_subject/{subjectId}", "Add/Edit Subject", Icons.Outlined.MenuBook) {
        fun createRoute(subjectId: Int) = "add_edit_subject/$subjectId"
    }
    object AddEditTask : Screen("add_edit_task/{taskId}", "Add/Edit Task", Icons.Outlined.Assignment) {
        fun createRoute(taskId: Int) = "add_edit_task/$taskId"
    }
    object AddEditExam : Screen("add_edit_exam/{examId}", "Add/Edit Exam", Icons.Outlined.School) {
        fun createRoute(examId: Int) = "add_edit_exam/$examId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Tasks,
    Screen.Subjects,
    Screen.Exams,
    Screen.AiAssistant
)
