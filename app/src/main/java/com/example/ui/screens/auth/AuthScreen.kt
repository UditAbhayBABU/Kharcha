package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TactileButton
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onSignIn: (email: String, pass: String) -> Unit,
    onSignUp: (email: String, pass: String, name: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onGoogleSignIn: (() -> Unit)? = null,
    isLoading: Boolean,
    errorMessage: String?
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KHARCHA BRAND LOGO
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(2.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₹",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
            }

            Text(
                text = "KHARCHA",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
            Text(
                text = "Khata Aur Kharche Ka Sahi Hisab",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = RedExpense.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedExpense)
                ) {
                    Text(
                        text = errorMessage,
                        color = RedExpense,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isRegisterMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Aapka Naam", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary
                    )
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = GoldPrimary
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = TextMuted) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                    val desc = if (passwordVisible) "Password chupayein" else "Password dekhein"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = if (passwordVisible) GoldPrimary else TextSecondary
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = GoldPrimary
                )
            )

            if (!isRegisterMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotDialog = true }) {
                        Text("Password Bhool Gaye?", color = GoldLight, fontSize = 12.sp)
                    }
                }
            }

            TactileButton(
                text = if (isRegisterMode) "Account Banao" else "Login Karo",
                onClick = {
                    if (isRegisterMode) {
                        onSignUp(email.trim(), password.trim(), name.trim())
                    } else {
                        onSignIn(email.trim(), password.trim())
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (onGoogleSignIn != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    Text(
                        text = "  YA  ",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                }

                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MetallicRimBrush),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google se Login Karein",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    text = if (isRegisterMode) "Pehle se account hai? Login Karo" else "Naya account banana hai? Register Karo",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (showForgotDialog) {
            var resetEmail by remember { mutableStateOf(email) }
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Password Reset Link", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Apna registered email daalo, password reset ka link bhej diya jayega.", color = TextSecondary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetEmail.isNotBlank()) {
                                onForgotPassword(resetEmail.trim())
                                showForgotDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold)
                    ) {
                        Text("Link Bhejo", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) { Text("Ruko", color = TextSecondary) }
                }
            )
        }
    }
}
