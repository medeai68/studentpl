package com.example.studentpl.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.studentpl.model.Subject
import com.example.studentpl.ui.theme.*
import com.example.studentpl.viewmodel.PlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(viewModel: PlannerViewModel, subjectId: Int, onBack: () -> Unit) {
    val subjects by viewModel.allSubjects.observeAsState(initial = emptyList())
    val subjectToEdit = subjects.find { it.id == subjectId }

    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var selectedColor by remember { mutableStateOf(subjectToEdit?.color ?: "#6C5CE7") }

    val modernColors = listOf(
        "#6C5CE7", "#00B8D4", "#FF6B6B", "#34D399", "#FBBF24",
        "#F472B6", "#FB923C", "#60A5FA", "#A78BFA", "#4ECDC4"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (subjectId == 0) "New Subject" else "Edit Subject", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                if (subjectId == 0) viewModel.insertSubject(Subject(name = name, color = selectedColor))
                                else viewModel.updateSubject(Subject(id = subjectId, name = name, color = selectedColor))
                                onBack()
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Preview circle
            val previewColor = try { Color(selectedColor.toColorInt()) } catch (_: Exception) { Color(0xFF6C5CE7) }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(previewColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (name.isNotEmpty()) name.take(2).uppercase() else "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Subject Name") },
                leadingIcon = { Icon(Icons.Default.MenuBook, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Text("Choose Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(modernColors) { colorHex ->
                    val color = Color(colorHex.toColorInt())
                    val isSelected = selectedColor == colorHex
                    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, spring(), label = "color_scale")

                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 52.dp else 48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = colorHex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (subjectId == 0) viewModel.insertSubject(Subject(name = name, color = selectedColor))
                        else viewModel.updateSubject(Subject(id = subjectId, name = name, color = selectedColor))
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(if (subjectId == 0) Icons.Default.Add else Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (subjectId == 0) "Create Subject" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
