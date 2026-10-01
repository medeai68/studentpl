package com.example.studentpl

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.studentpl.navigation.Screen
import com.example.studentpl.navigation.bottomNavItems
import com.example.studentpl.ui.screens.*
import com.example.studentpl.ui.theme.*
import com.example.studentpl.viewmodel.PlannerViewModel
import com.example.studentpl.viewmodel.PlannerViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: PlannerViewModel by viewModels {
        val app = application as PlannerApplication
        PlannerViewModelFactory(
            app.repository, app.aiRepository,
            app.notificationScheduler, app.themePreferenceManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDarkTheme = when (appTheme) {
                com.example.studentpl.data.preferences.AppTheme.LIGHT -> false
                com.example.studentpl.data.preferences.AppTheme.DARK -> true
                com.example.studentpl.data.preferences.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            StudentplTheme(darkTheme = isDarkTheme) {
                MainScreen(viewModel)
            }
        }
    }
}

// Routes where the bottom nav bar should be hidden
private val authRoutes = setOf(Screen.Login.route, Screen.Signup.route)

@Composable
fun MainScreen(viewModel: PlannerViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val user by viewModel.user.observeAsState()

    // Track whether user data has been loaded (distinguish "no user" from "still loading")
    var userLoaded by remember { mutableStateOf(false) }

    // Detect when user data arrives from the database
    LaunchedEffect(user) {
        // user is null both when: (a) still loading, (b) no user in DB
        // The first non-null emission, or a null after we've seen non-null, means data is loaded
        userLoaded = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Auto-login: if user is already in the local DB, skip login/signup screens
    LaunchedEffect(userLoaded) {
        if (userLoaded && user != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Redirect to Login when the user logs out (becomes null after being loaded)
    LaunchedEffect(user) {
        if (userLoaded && user == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        (context.applicationContext as PlannerApplication).notificationScheduler.scheduleDailySummary()
    }

    // Track current route to know if we should show the bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = user != null && currentRoute !in authRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it },
                    exit = slideOutVertically(tween(300)) { it }
                ) {
                    ModernBottomBar(navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(300)) + slideInVertically(tween(400)) { 100 } },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(viewModel = viewModel,
                    onSignupSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel,
                    onAddTask = { navController.navigate(Screen.AddEditTask.createRoute(0)) },
                    onNavigateToAi = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(viewModel = viewModel,
                    onAddTask = { navController.navigate(Screen.AddEditTask.createRoute(0)) },
                    onEditTask = { taskId -> navController.navigate(Screen.AddEditTask.createRoute(taskId)) }
                )
            }
            composable(Screen.Exams.route) {
                ExamsScreen(viewModel = viewModel,
                    onAddExam = { navController.navigate(Screen.AddEditExam.createRoute(0)) },
                    onEditExam = { examId -> navController.navigate(Screen.AddEditExam.createRoute(examId)) }
                )
            }
            composable(Screen.Subjects.route) {
                SubjectsScreen(viewModel = viewModel,
                    onAddSubject = { navController.navigate(Screen.AddEditSubject.createRoute(0)) },
                    onEditSubject = { subjectId -> navController.navigate(Screen.AddEditSubject.createRoute(subjectId)) }
                )
            }
            composable(Screen.AiAssistant.route) { AiAssistantScreen(viewModel = viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            composable(
                route = Screen.AddEditSubject.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0
                AddEditSubjectScreen(viewModel = viewModel, subjectId = subjectId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.AddEditTask.route,
                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
                AddEditTaskScreen(viewModel = viewModel, taskId = taskId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.AddEditExam.route,
                arguments = listOf(navArgument("examId") { type = NavType.IntType })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getInt("examId") ?: 0
                AddEditExamScreen(viewModel = viewModel, examId = examId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun ModernBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .height(72.dp)
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) screen.selectedIcon else screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    AnimatedVisibility(
                        visible = selected,
                        enter = expandVertically(spring()) + fadeIn(),
                        exit = shrinkVertically(spring()) + fadeOut()
                    ) {
                        Text(screen.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                },
                selected = selected,
                onClick = {
                    if (screen.route == Screen.Home.route) {
                        // Pop everything above Home (like Profile) and return to it
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    } else {
                        navController.navigate(screen.route) {
                            // Pop back to Home (not inclusive) so Home stays as the root
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
