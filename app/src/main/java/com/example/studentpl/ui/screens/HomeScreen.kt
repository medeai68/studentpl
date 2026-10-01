package com.example.studentpl.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studentpl.model.Task
import com.example.studentpl.ui.theme.*
import com.example.studentpl.viewmodel.PlannerViewModel
import com.example.studentpl.data.preferences.AppTheme
import java.time.LocalDate

// ── Spring animation specs ──────────────────────────────────────────────────
private val springSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)
@Composable
fun HomeScreen(
    viewModel: PlannerViewModel,
    onAddTask: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val tasks by viewModel.allTasks.observeAsState(initial = emptyList())
    val exams by viewModel.allExams.observeAsState(initial = emptyList())
    val user by viewModel.user.observeAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    val today = LocalDate.now().toString()
    val todayTasks = tasks.filter { it.dueDate == today && !it.completed }
    val pendingTasks = tasks.filter { !it.completed }
    val completedTasks = tasks.filter { it.completed }
    val progress = if (tasks.isNotEmpty()) completedTasks.size.toFloat() / tasks.size else 0f

    // Entrance animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = springSpec) + fadeIn(animationSpec = tween(400))
            ) {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero Header ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedVisibility(
                    visible =
                        visible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(600)) { -40 }
                ) {
                    GreetingHeader(
                        userName = user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Student",
                        subtitle = getGreetingSubtitle(),
                        currentTheme = appTheme,
                        onThemeCycle = {
                            val next = when (appTheme) {
                                AppTheme.LIGHT -> AppTheme.DARK
                                AppTheme.DARK -> AppTheme.SYSTEM
                                AppTheme.SYSTEM -> AppTheme.LIGHT
                            }
                            viewModel.setTheme(next)
                        },
                        onProfileClick = onNavigateToProfile
                    )
                }
            }

            // ── Streak + Progress Row ───────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(600)) { -20 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StreakCard(
                            modifier = Modifier.weight(1f),
                            streakDays = completedTasks.size.coerceAtMost(7)
                        )
                        GradientProgressCard(
                            modifier = Modifier.weight(1f),
                            progress = progress,
                            completed = completedTasks.size,
                            total = tasks.size
                        )
                    }
                }
            }

            // ── Quick Actions ───────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -10 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AutoAwesome,
                            title = "Ask AI",
                            subtitle = "Get study help",
                            gradientColors = listOf(GradientCyanStart, GradientCyanEnd),
                            onClick = onNavigateToAi
                        )
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AddCircle,
                            title = "New Task",
                            subtitle = "Stay organized",
                            gradientColors = listOf(GradientPurpleStart, GradientPurpleEnd),
                            onClick = onAddTask
                        )
                    }
                }
            }

            // ── Stats Row ──────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(600)) { -10 }
                ) {
                    SectionLabel("Overview")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Assignment,
                        label = "Pending",
                        value = "${pendingTasks.size}",
                        color = Tertiary
                    )
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Book,
                        label = "Exams",
                        value = "${exams.size}",
                        color = Secondary
                    )
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = "Done",
                        value = "${completedTasks.size}",
                        color = AccentGreen
                    )
                }
            }

            // ── Today's Tasks ───────────────────────────────────────────────
            if (todayTasks.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800))
                    ) {
                        SectionLabel("Today's Focus")
                    }
                }
                items(todayTasks, key = { it.id }) { task ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(600)) { 30 }
                    ) {
                        ModernTaskCard(
                            task = task,
                            onComplete = { viewModel.updateTask(task.copy(completed = true)) }
                        )
                    }
                }
            }

            // ── Upcoming Exams ──────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(900))
                ) {
                    SectionLabel("Upcoming Exams")
                }
            }
            if (exams.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.School,
                        message = "No upcoming exams. You're all caught up!"
                    )
                }
            }
            items(exams.take(4), key = { it.id }) { exam ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(600)) { 30 }
                ) {
                    ExamPreviewCard(exam)
                }
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ── Greeting Header ─────────────────────────────────────────────────────────
@Composable
private fun GreetingHeader(
    userName: String,
    subtitle: String,
    currentTheme: AppTheme,
    onThemeCycle: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello, $userName",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Theme cycle button
        IconButton(
            onClick = onThemeCycle,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = when (currentTheme) {
                    AppTheme.LIGHT -> Icons.Default.LightMode
                    AppTheme.DARK -> Icons.Default.DarkMode
                    AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                },
                contentDescription = "Toggle theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(GradientPurpleStart, GradientCyanEnd),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    )
                )
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ── Streak Card ─────────────────────────────────────────────────────────────
@Composable
private fun StreakCard(modifier: Modifier = Modifier, streakDays: Int) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$streakDays",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Day Streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Gradient Progress Card ──────────────────────────────────────────────────
@Composable
private fun GradientProgressCard(
    modifier: Modifier = Modifier,
    progress: Float,
    completed: Int,
    total: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, delayMillis = 400),
        label = "progress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$completed / $total tasks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Quick Action Card ───────────────────────────────────────────────────────
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ── Mini Stat ───────────────────────────────────────────────────────────────
@Composable
private fun MiniStat(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Modern Task Card ────────────────────────────────────────────────────────
@Composable
private fun ModernTaskCard(task: Task, onComplete: () -> Unit) {
    var checked by remember { mutableStateOf(task.completed) }
    val scale by animateFloatAsState(
        targetValue = if (checked) 0.95f else 1f,
        animationSpec = springSpec,
        label = "task_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    checked = true
                    onComplete()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (task.completed) AccentGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (task.dueDate.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        task.dueDate.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Exam Preview Card ───────────────────────────────────────────────────────
@Composable
private fun ExamPreviewCard(exam: com.example.studentpl.model.Exam) {
    val daysRemaining = calculateDaysRemaining(exam.examDate)
    val urgencyColor = when {
        daysRemaining < 0 -> MaterialTheme.colorScheme.outline
        daysRemaining <= 3 -> MaterialTheme.colorScheme.error
        daysRemaining <= 7 -> AccentOrange
        else -> AccentGreen
    }
    val daysText = when {
        daysRemaining < 0 -> "Past"
        daysRemaining == 0L -> "Today!"
        daysRemaining == 1L -> "Tomorrow"
        else -> "${daysRemaining}d left"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject initial circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientCyanEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    exam.subject.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exam.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        exam.examDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = urgencyColor.copy(alpha = 0.12f)
            ) {
                Text(
                    daysText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = urgencyColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────────────
@Composable
private fun EmptyStateCard(icon: ImageVector, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Section Label ───────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

// ── Helpers ─────────────────────────────────────────────────────────────────
private fun getGreetingSubtitle(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "☀️ Rise and shine! Let's study."
        in 12..16 -> "🌤️ Good afternoon! Stay focused."
        in 17..20 -> "🌅 Good evening! Wrap it up."
        else -> "🌙 Late night grind! You got this."
    }
}
