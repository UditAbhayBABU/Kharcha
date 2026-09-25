package com.example.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TactileButton
import com.example.ui.components.TactileNumberPad
import com.example.ui.theme.*

@Composable
fun PinLockScreen(
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onOpenQuickAdd: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.5.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "KHARCHA Lock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = if (isError) "Galat PIN! Phir se koshish karein." else "App kholne ke liye PIN daalo",
                fontSize = 13.sp,
                color = if (isError) RedExpense else TextSecondary
            )

            // PIN Dots Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 1..4) {
                    val filled = enteredPin.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) (if (isError) RedExpense else GoldPrimary) else SurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (filled) (if (isError) RedExpense else GoldPrimary) else BorderMedium,
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactile Number Pad
            TactileNumberPad(
                onDigitClick = { digit ->
                    if (digit != "." && enteredPin.length < 4) {
                        enteredPin += digit
                        isError = false
                        if (enteredPin.length == 4) {
                            val ok = onVerifyPin(enteredPin)
                            if (ok) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                enteredPin = ""
                            }
                        }
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        isError = false
                    }
                },
                onClear = {
                    enteredPin = ""
                    isError = false
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Add Shortcut Button directly on Lock Screen
            TactileButton(
                text = "Jaldi Kharcha Jodo (No PIN)",
                icon = Icons.Default.Add,
                onClick = onOpenQuickAdd,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
