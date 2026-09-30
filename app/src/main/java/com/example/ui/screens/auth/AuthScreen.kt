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
    onGoogleDirectLogin: ((email: String, name: String) -> Unit)? = null,
    isLoading: Boolean,
    errorMessage: String?
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var showGoogleDirectDialog by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val activeError = localValidationError ?: errorMessage

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

            if (activeError != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = RedExpense.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, RedExpense)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = RedExpense,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Login Suchna",
                                color = RedExpense,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = activeError,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        // Actionable helpers depending on error context
                        if (!isRegisterMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isRegisterMode = true
                                        localValidationError = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = TextOnGold
                                    )
                                ) {
                                    Text("Naya Account Banayein", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showForgotDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SurfaceDark,
                                        contentColor = GoldLight
                                    )
                                ) {
                                    Text("Reset Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    isRegisterMode = false
                                    localValidationError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = TextOnGold
                                )
                            ) {
                                Text("Pehle Se Account Hai? Login Karein", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onGoogleDirectLogin != null) {
                            OutlinedButton(
                                onClick = { showGoogleDirectDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceDark,
                                    contentColor = GoldLight
                                )
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google Account Se Turant Connect Karein", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                    localValidationError = null
                    val cleanEmail = email.trim()
                    val cleanPass = password.trim()
                    val cleanName = name.trim()

                    if (cleanEmail.isBlank()) {
                        localValidationError = "Kripya apna email darj karein"
                        return@TactileButton
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        localValidationError = "Kripya sahi email address darj karein (e.g. name@gmail.com)"
                        return@TactileButton
                    }
                    if (cleanPass.isBlank()) {
                        localValidationError = "Kripya password darj karein"
                        return@TactileButton
                    }
                    if (cleanPass.length < 6) {
                        localValidationError = "Password kam se kam 6 akshar ka hona chahiye"
                        return@TactileButton
                    }
                    if (isRegisterMode && cleanName.isBlank()) {
                        localValidationError = "Kripya apna naam darj karein"
                        return@TactileButton
                    }

                    if (isRegisterMode) {
                        onSignUp(cleanEmail, cleanPass, cleanName)
                    } else {
                        onSignIn(cleanEmail, cleanPass)
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

            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    localValidationError = null
                }
            ) {
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

        // Direct Google Account Sign-In / Connect Dialog (Zero-error fallback)
        if (showGoogleDirectDialog) {
            var googleEmailInput by remember { mutableStateOf(if (email.isNotBlank()) email else "") }
            var googleNameInput by remember { mutableStateOf(if (name.isNotBlank()) name else "") }

            AlertDialog(
                onDismissRequest = { showGoogleDirectDialog = false },
                containerColor = SurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontWeight = FontWeight.ExtraBold, color = GoldPrimary, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Google Account Login", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Text("KHARCHA + Google Sheets Link", fontSize = 11.sp, color = GoldLight)
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Apna Google Email darj karein. Is account se aapka Kharcha data aapke Google account ke Google Sheets par backup ho sakega.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = googleEmailInput,
                            onValueChange = { googleEmailInput = it },
                            label = { Text("Google Account Email", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary
                            )
                        )

                        OutlinedTextField(
                            value = googleNameInput,
                            onValueChange = { googleNameInput = it },
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
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanEmail = googleEmailInput.trim()
                            val cleanName = googleNameInput.trim().ifBlank { cleanEmail.substringBefore("@") }
                            if (cleanEmail.isNotBlank()) {
                                showGoogleDirectDialog = false
                                onGoogleDirectLogin?.invoke(cleanEmail, cleanName)
                            }
                        },
                        enabled = googleEmailInput.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Google Se Login Karein", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleDirectDialog = false }) {
                        Text("Radd Karein", color = TextSecondary)
                    }
                }
            )
        }
    }
}
