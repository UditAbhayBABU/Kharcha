package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun KharchaCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    borderColor: Color = BorderSubtle,
    backgroundColor: Color = SurfaceCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), shape),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "button_press")

    val bgGradient = if (isPrimary) {
        Brush.verticalGradient(listOf(GoldPrimary, GoldDark))
    } else {
        Brush.verticalGradient(listOf(SurfaceElevated, SurfaceCard))
    }

    val textColor = if (isPrimary) TextOnGold else TextPrimary
    val borderColor = if (isPrimary) GoldLight.copy(alpha = 0.4f) else BorderMedium

    Box(
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) bgGradient else Brush.linearGradient(listOf(SurfaceCard, SurfaceDark)))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun TactileChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = GoldPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "chip_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.18f) else SurfaceElevated
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    if (selected) accentColor else BorderSubtle
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (selected) TextPrimary else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TactileNumberPad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (key == "⌫") SurfaceDark else SurfaceElevated
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (key == "⌫") BorderMedium else BorderSubtle
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (key == "⌫") onBackspace() else onDigitClick(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Mitao",
                                tint = OrangeWarning,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HumorousBudgetDialog(
    categoryName: String,
    limit: Double,
    currentSpent: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        titleContentColor = OrangeWarning,
        textContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = OrangeWarning,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Budget Alert!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "“Iss mahine already bhot kharcha ho gaya MALIKK!”",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = GoldLight
                )
                Text(
                    text = "$categoryName me limit ₹${limit.toInt()} thi, aur ab tak ₹${currentSpent.toInt()} kharch ho chuke hain!",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Samajh Gaya Malikk", color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ExcelWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "Excel Update",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Excel par file khuli nahi hai na?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldLight
                )
                Text(
                    text = "Agar PC par workbook open hogi toh sync me clash aa sakta hai.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ruko", color = TextSecondary)
            }
        }
    )
}
