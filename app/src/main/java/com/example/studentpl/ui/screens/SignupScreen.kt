package com.example.studentpl.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studentpl.ui.theme.*
import com.example.studentpl.viewmodel.PlannerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignupScreen(
    viewModel: PlannerViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(viewModel.signupStatus) {
        viewModel.signupStatus.collectLatest { status ->
            isLoading = false
            errorMessage = when (status) {
                "SUCCESS" -> { onSignupSuccess(); null }
                "EMAIL_EXISTS" -> "This email is already registered"
                "FIELDS_EMPTY" -> "Please fill in all fields"
                "CONNECTION_ERROR" -> "Cannot connect to server.\nRun: adb reverse tcp:3306 tcp:3306"
                "DATABASE_ERROR" -> "A database error occurred"
                else -> "Signup failed: $status"
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = -500f, targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bg_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    start = Offset(bgOffset, -300f),
                    end = Offset(300f, bgOffset + 400f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(600))
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GradientPurpleStart, GradientCoralStart)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(600)) { 20 }
            ) {
                Text(
                    "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(600)) { 20 }
            ) {
                Text(
                    "Join and ace your studies",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            val fieldModifier = Modifier.fillMaxWidth()
            val fieldShape = RoundedCornerShape(14.dp)
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                cursorColor = MaterialTheme.colorScheme.primary
            )

            // Email
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(900)) + slideInVertically(tween(600)) { 30 }) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email address") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = fieldModifier, shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true, colors = fieldColors
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Password
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1000)) + slideInVertically(tween(600)) { 30 }) {
                OutlinedTextField(
                    value = password, onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = fieldModifier, shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true, colors = fieldColors
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1100)) + slideInVertically(tween(600)) { 30 }) {
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = fieldModifier, shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true, colors = fieldColors
                )
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    errorMessage ?: "", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1200)) + slideInVertically(tween(600)) { 30 }) {
                Button(
                    onClick = {
                        if (password != confirmPassword) { errorMessage = "Passwords do not match"; return@Button }
                        isLoading = true; errorMessage = null
                        viewModel.signup(email, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    AnimatedContent(targetState = isLoading, transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }, label = "btn") { loading ->
                        if (loading) CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                        )
                        else Text("Create Account", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1300))) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sign In", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
