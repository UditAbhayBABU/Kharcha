package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Modern Dark Tactile / Glossy Glassmorphic card with metallic rim highlights and obsidian depth.
 */
@Composable
fun KharchaCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderColor: Color = BorderSubtle,
    backgroundColor: Color = SurfaceCard,
    isGlossy: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val rimBrush = if (isGlossy) MetallicRimBrush else Brush.linearGradient(listOf(borderColor, borderColor))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.2.dp, rimBrush), shape),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Box {
            // Specular top rim shine for glossy glassmorphic depth
            if (isGlossy) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GlassRimTop,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

/**
 * Tactile Button with spring micro-interaction, haptic feedback, and glossy metallic highlight.
 */
@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentColor: Color = GoldPrimary
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_spring_scale"
    )

    val bgGradient = if (isPrimary) {
        Brush.verticalGradient(
            listOf(
                accentColor,
                GoldDark
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                SurfaceElevated,
                SurfaceDark
            )
        )
    }

    val textColor = if (isPrimary) TextOnGold else TextPrimary
    val rimBorderBrush = if (isPrimary) GoldMetallicRimBrush else MetallicRimBrush

    Box(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) bgGradient else Brush.linearGradient(listOf(SurfaceCard, SurfaceDark)))
            .border(BorderStroke(1.2.dp, if (enabled) rimBorderBrush else BorderSubtle.let { Brush.linearGradient(listOf(it, it)) }), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isPrimary) GoldLight else Color.White),
                enabled = enabled && !isLoading,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle specular highlight on top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = if (isPrimary) 0.45f else 0.2f), Color.Transparent)
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = textColor,
                    strokeWidth = 2.5.dp
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
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Tactile Chip with spring micro-interaction and metallic active border.
 */
@Composable
fun TactileChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = GoldPrimary
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "chip_spring"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(SurfaceElevated, SurfaceDark)
                    )
                }
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    if (selected) {
                        Brush.linearGradient(
                            listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.5f),
                                Color(0x66FFFFFF)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(BorderSubtle, BorderSubtle))
                    }
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                color = if (selected) TextPrimary else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Tactile Keypad with spring bounce and haptic click on each number.
 */
@Composable
fun TactileNumberPad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
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
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val keyScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.90f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "pad_key_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(keyScale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (key == "⌫") {
                                    Brush.verticalGradient(listOf(SurfaceDark, Color(0xFF140D0F)))
                                } else {
                                    Brush.verticalGradient(listOf(SurfaceElevated, SurfaceDark))
                                }
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (key == "⌫") {
                                        OrangeWarning.copy(alpha = 0.4f)
                                    } else {
                                        BorderSubtle
                                    }
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(color = if (key == "⌫") OrangeWarning else GoldPrimary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (key == "⌫") onBackspace() else onDigitClick(key)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Top rim specular highlight for tactile depth
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent)
                                    )
                                )
                        )

                        if (key == "⌫") {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Mitao",
                                tint = OrangeWarning,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                color = TextPrimary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
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
    alreadySpent: Double,
    currentExpense: Double,
    projectedSpent: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = OrangeWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Budget Alert", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$categoryName ki monthly budget limit exceed ho rahi hai.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Budget Limit: ₹${limit.toInt()}",
                    fontSize = 13.sp,
                    color = GoldLight,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ab tak: ₹${alreadySpent.toInt()}  •  Naya: ₹${currentExpense.toInt()}  •  Total: ₹${projectedSpent.toInt()}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "✓ Kharcha record ho gaya hai.",
                    fontSize = 12.sp,
                    color = EmeraldCash,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = GoldPrimary, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(20.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                shape = RoundedCornerShape(12.dp)
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
