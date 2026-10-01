package com.example.studentpl.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studentpl.ui.theme.*
import com.example.studentpl.viewmodel.PlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: PlannerViewModel) {
    val user by viewModel.user.observeAsState()
    val tasks by viewModel.allTasks.observeAsState(initial = emptyList())
    val exams by viewModel.allExams.observeAsState(initial = emptyList())
    val subjects by viewModel.allSubjects.observeAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.syncStatus.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val completedTasks = tasks.count { it.completed }
    val progressPct = if (tasks.isNotEmpty()) (completedTasks * 100 / tasks.size) else 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Animated Avatar
            item {
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    1f, 1.08f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "av_pulse"
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(GradientPurpleStart, GradientCyanEnd),
                                start = Offset(0f, 0f), end = Offset(1f, 1f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user?.email?.take(2)?.uppercase() ?: "??",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // User info
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Student",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stats Grid
            item {
                SectionLabel("Academic Stats")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(Modifier.weight(1f), "Tasks Done", "$completedTasks/${tasks.size}", Icons.AutoMirrored.Filled.Assignment, GradientPurpleStart)
                    ProfileStatCard(Modifier.weight(1f), "Subjects", "${subjects.size}", Icons.Default.Book, GradientCyanStart)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(Modifier.weight(1f), "Exams", "${exams.size}", Icons.Default.Grade, GradientCoralStart)
                    ProfileStatCard(Modifier.weight(1f), "Progress", "$progressPct%", Icons.Default.TrendingUp, AccentGreen)
                }
            }

            // Progress bar
            if (tasks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overall Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("$progressPct%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(12.dp))
                            val animProgress by animateFloatAsState(progressPct / 100f, tween(1000), label = "prof_prog")
                            LinearProgressIndicator(
                                progress = { animProgress },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        }
                    }
                }
            }

            // Sync button
            item {
                Button(
                    onClick = { viewModel.syncAll() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync with Cloud", style = MaterialTheme.typography.titleSmall)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ProfileStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()
    )
}
