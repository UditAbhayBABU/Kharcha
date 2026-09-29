package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

/**
 * Supported celebration action types across the app.
 */
enum class CelebrationActionType {
    EXPENSE_SAVED,
    UDHAAR_RECORDED,
    UDHAAR_SETTLED,
    POT_DEPOSIT,
    POT_CREATED,
    EXPENSE_DELETED,
    SYNC_SUCCESS,
    SETTINGS_SAVED
}

/**
 * Data bundle for triggering a celebration.
 */
data class CelebrationData(
    val id: Long = System.currentTimeMillis(),
    val type: CelebrationActionType,
    val title: String,
    val subtitle: String? = null,
    val amount: Double? = null
)

/**
 * Controller to trigger celebrations from anywhere in the app.
 */
@Stable
class CelebrationController {
    var activeCelebration by mutableStateOf<CelebrationData?>(null)
        private set

    fun celebrate(
        type: CelebrationActionType,
        title: String,
        subtitle: String? = null,
        amount: Double? = null
    ) {
        activeCelebration = CelebrationData(
            id = System.currentTimeMillis(),
            type = type,
            title = title,
            subtitle = subtitle,
            amount = amount
        )
    }

    fun dismiss() {
        activeCelebration = null
    }
}

val LocalCelebrationController = staticCompositionLocalOf { CelebrationController() }

/**
 * Subtle Haptic Helper:
 * Delivers refined, premium, micro-tactile vibrations (subtle click & tick)
 * rather than aggressive or heavy long buzzes.
 */
object SubtleHapticHelper {
    fun playSubtleEntrance(context: Context, hapticFeedback: HapticFeedback) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(14, 50))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    fun playSubtleTickComplete(context: Context, hapticFeedback: HapticFeedback) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(10, 38))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(8)
                }
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }
}

/**
 * Native Android PCM Audio Synthesizer (0 External Assets / 0 Network Calls).
 * Synthesizes sweet tactile chimes matching the visual celebration.
 */
object AudioSynthesizer {
    private const val SAMPLE_RATE = 44100

    suspend fun playForType(type: CelebrationActionType) = withContext(Dispatchers.Default) {
        when (type) {
            CelebrationActionType.EXPENSE_SAVED -> {
                // Ascending melodic arpeggio C6 (1046.5Hz) -> E6 (1318.5Hz) -> G6 (1567.9Hz) -> C7 (2093.0Hz)
                playArpeggio(doubleArrayOf(1046.5, 1318.5, 1567.9, 2093.0), noteDurationMs = 50, decay = 8.0)
            }
            CelebrationActionType.UDHAAR_RECORDED -> {
                // Double affirmative chime E6 -> A6
                playArpeggio(doubleArrayOf(1318.5, 1760.0), noteDurationMs = 70, decay = 7.5)
            }
            CelebrationActionType.UDHAAR_SETTLED -> {
                // Triumphant major chord D6 -> F#6 -> A6 -> D7
                playArpeggio(doubleArrayOf(1174.6, 1479.9, 1760.0, 2349.3), noteDurationMs = 55, decay = 7.0)
            }
            CelebrationActionType.POT_DEPOSIT, CelebrationActionType.POT_CREATED -> {
                // Sparkling crystalline chime G6 -> C7 -> E7
                playArpeggio(doubleArrayOf(1567.9, 2093.0, 2637.0), noteDurationMs = 60, decay = 9.0)
            }
            CelebrationActionType.EXPENSE_DELETED -> {
                // Cute bubbly pop (downward frequency sweep from 540Hz down to 220Hz)
                playBubblyPop(startFreq = 540.0, endFreq = 220.0, durationMs = 110)
            }
            CelebrationActionType.SYNC_SUCCESS -> {
                // Harmonious uplifting chord E5 -> B5 -> G#6
                playArpeggio(doubleArrayOf(659.25, 987.77, 1661.22), noteDurationMs = 65, decay = 6.5)
            }
            CelebrationActionType.SETTINGS_SAVED -> {
                // Crisp click-chime A6 -> C#7
                playArpeggio(doubleArrayOf(1760.0, 2217.4), noteDurationMs = 50, decay = 9.0)
            }
        }
    }

    private fun playArpeggio(frequencies: DoubleArray, noteDurationMs: Int, decay: Double) {
        try {
            val totalNotes = frequencies.size
            val noteSamples = (SAMPLE_RATE * (noteDurationMs / 1000.0)).toInt()
            val totalSamples = noteSamples * totalNotes + (SAMPLE_RATE * 0.12).toInt()
            val buffer = ShortArray(totalSamples)

            for (n in frequencies.indices) {
                val freq = frequencies[n]
                val startIdx = n * noteSamples
                val maxLen = totalSamples - startIdx
                for (i in 0 until maxLen) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = exp(-decay * t)
                    val sine = sin(2.0 * PI * freq * t)
                    val overtone = sin(4.0 * PI * freq * t) * 0.25
                    val sampleValue = (sine + overtone) * envelope * Short.MAX_VALUE * 0.38
                    val curr = buffer[startIdx + i]
                    val mixed = (curr + sampleValue.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    buffer[startIdx + i] = mixed.toShort()
                }
            }
            playPcm(buffer)
        } catch (_: Exception) {
        }
    }

    private fun playBubblyPop(startFreq: Double, endFreq: Double, durationMs: Int) {
        try {
            val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)
            var phase = 0.0
            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples
                val currentFreq = startFreq + (endFreq - startFreq) * (progress * progress)
                phase += 2.0 * PI * currentFreq / SAMPLE_RATE
                val envelope = (1.0 - progress) * exp(-3.5 * progress)
                val sampleValue = (sin(phase) * envelope * Short.MAX_VALUE * 0.42).toInt()
                buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            Thread {
                try {
                    Thread.sleep((buffer.size * 1000L / SAMPLE_RATE) + 80)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {
        }
    }
}

/**
 * Particle representation for Apple 3D glossy emoji burst.
 */
private data class GlossyEmojiParticle(
    val emojiKind: GlossyEmojiKind,
    val angleRad: Float,
    val targetDistance: Float,
    val spinAngle: Float,
    val sizeDp: Float
)

/**
 * Types of 3D Volumetric Apple-style glossy emojis drawn on Canvas.
 */
private enum class GlossyEmojiKind {
    HONEY_PARTY_FACE,      // 🥳 Volumetric honey-yellow glossy face with party hat
    VOLUMETRIC_MONEY_WINGS,// 💸 Glossy cash with 3D wings
    GOLD_COIN_3D,          // 🪙 Thick 3D gold coin with rupee emboss & specular rim
    SPARKLE_STAR_3D,       // ✨ 4-point glowing diamond star
    DIAMOND_GEM_3D,        // 💎 Glossy crystal cyan gem
    FIRE_FLAME_3D,         // 🔥 Curved 3D teardrop flame
    RUBY_HEART_3D,         // ❤️ Apple-style volumetric glossy ruby heart
    COOL_SHADES_FACE,      // 😎 Honey-yellow face with shiny sunglasses
    POOF_SMOKE_BUBBLE,     // 💨 Bubbly smoke puff for deletes
    ROCKET_3D              // 🚀 3D sleek rocket for sync
}

/**
 * Master Action Celebration Overlay:
 * - Subtle tactile haptics (pop on entrance + crisp micro-tick on checkmark completion).
 * - Subtle concentric glass ripples radiating from behind the jewel badge.
 * - Seamless PathMeasure-based dynamic animated checkmark with soft specular glow.
 * - 3-4 Emojis bursting in all 360-degree directions (omnidirectional radial explosion).
 * - Buttery smooth 2.5s sequence with a luxurious, non-abrupt exit fade and slide.
 */
@Composable
fun ActionCelebrationOverlay(
    celebration: CelebrationData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val totalDurationMs = 2500

    // 1. Subtle Haptic & Audio on Entrance
    LaunchedEffect(celebration.id) {
        SubtleHapticHelper.playSubtleEntrance(context, haptic)
        AudioSynthesizer.playForType(celebration.type)
        delay(totalDurationMs.toLong())
        onDismiss()
    }

    // 2. Master Sequence Progress (0f -> 1f) over 2500ms
    val sequenceProgress = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        sequenceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalDurationMs, easing = LinearEasing)
        )
    }

    // 3. Playful Spring Overshoot Entrance: 0f -> 1.15f -> 1.0f
    val jewelScale = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        jewelScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.58f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    // 4. Subtle Concentric Glass Ripples (Radiating shockwaves)
    val ripple1Progress = remember { Animatable(0f) }
    val ripple2Progress = remember { Animatable(0f) }

    LaunchedEffect(celebration.id) {
        delay(120)
        ripple1Progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(celebration.id) {
        delay(260)
        ripple2Progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
    }

    // 5. Dynamic Animated "Tick" (Checkmark) Drawing Progress (0f -> 1f)
    val tickProgress = remember { Animatable(0f) }
    val tickBounce = remember { Animatable(1f) }

    LaunchedEffect(celebration.id) {
        delay(180)
        tickProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
            )
        )
        // Secondary subtle micro-haptic & gentle bounce when tick completes!
        SubtleHapticHelper.playSubtleTickComplete(context, haptic)
        tickBounce.animateTo(
            targetValue = 1.08f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        tickBounce.animateTo(1.0f)
    }

    // 6. Gentle Levitation (floating sine wave)
    val infiniteTransition = rememberInfiniteTransition(label = "levitate")
    val levitationOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitate_y"
    )

    // Exactly 3 to 4 emojis bursting in ALL 4 diagonal directions (Omnidirectional 360-degree burst)
    val particles = remember(celebration.id) {
        val emojis = when (celebration.type) {
            CelebrationActionType.EXPENSE_SAVED -> listOf(
                GlossyEmojiKind.HONEY_PARTY_FACE,
                GlossyEmojiKind.GOLD_COIN_3D,
                GlossyEmojiKind.VOLUMETRIC_MONEY_WINGS,
                GlossyEmojiKind.SPARKLE_STAR_3D
            )
            CelebrationActionType.UDHAAR_RECORDED, CelebrationActionType.UDHAAR_SETTLED -> listOf(
                GlossyEmojiKind.GOLD_COIN_3D,
                GlossyEmojiKind.COOL_SHADES_FACE,
                GlossyEmojiKind.RUBY_HEART_3D,
                GlossyEmojiKind.SPARKLE_STAR_3D
            )
            CelebrationActionType.POT_DEPOSIT, CelebrationActionType.POT_CREATED -> listOf(
                GlossyEmojiKind.DIAMOND_GEM_3D,
                GlossyEmojiKind.GOLD_COIN_3D,
                GlossyEmojiKind.ROCKET_3D,
                GlossyEmojiKind.SPARKLE_STAR_3D
            )
            CelebrationActionType.EXPENSE_DELETED -> listOf(
                GlossyEmojiKind.POOF_SMOKE_BUBBLE,
                GlossyEmojiKind.HONEY_PARTY_FACE,
                GlossyEmojiKind.POOF_SMOKE_BUBBLE,
                GlossyEmojiKind.SPARKLE_STAR_3D
            )
            CelebrationActionType.SYNC_SUCCESS -> listOf(
                GlossyEmojiKind.ROCKET_3D,
                GlossyEmojiKind.SPARKLE_STAR_3D,
                GlossyEmojiKind.DIAMOND_GEM_3D,
                GlossyEmojiKind.HONEY_PARTY_FACE
            )
            CelebrationActionType.SETTINGS_SAVED -> listOf(
                GlossyEmojiKind.SPARKLE_STAR_3D,
                GlossyEmojiKind.COOL_SHADES_FACE,
                GlossyEmojiKind.DIAMOND_GEM_3D,
                GlossyEmojiKind.GOLD_COIN_3D
            )
        }

        val baseAngles = listOf(
            -PI.toFloat() * 0.22f, // Top-Right (~ -40°)
            -PI.toFloat() * 0.78f, // Top-Left (~ -140°)
            PI.toFloat() * 0.25f,  // Bottom-Right (~ +45°)
            PI.toFloat() * 0.75f   // Bottom-Left (~ +135°)
        )

        baseAngles.mapIndexed { idx, angle ->
            val dist = 145f + (idx % 2) * 20f
            GlossyEmojiParticle(
                emojiKind = emojis[idx % emojis.size],
                angleRad = angle,
                targetDistance = dist,
                spinAngle = if (idx % 2 == 0) 18f else -18f,
                sizeDp = 42f
            )
        }
    }

    val progress = sequenceProgress.value

    // ULTRA-SMOOTH OUT ANIMATION (Starts at 70% ~ 1750ms through 2500ms)
    val exitStart = 0.70f
    val exitProgress = if (progress < exitStart) {
        0f
    } else {
        ((progress - exitStart) / (1f - exitStart)).coerceIn(0f, 1f)
    }

    val smoothExitAlpha = (1f - exitProgress * exitProgress).coerceIn(0f, 1f)
    val exitSlideY = -30f * exitProgress
    val exitScale = 1.0f - (0.12f * exitProgress)

    val entranceAlpha = (progress / 0.12f).coerceIn(0f, 1f)
    val backdropAlpha = entranceAlpha * smoothExitAlpha * 0.42f

    // Fullscreen non-blocking transparent overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle dark vignette backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(backdropAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x80040711),
                            Color(0xD9010307)
                        )
                    )
                )
        )

        // Center Stack
        Box(
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer {
                    alpha = smoothExitAlpha
                    translationY = levitationOffset + exitSlideY
                    scaleX = exitScale
                    scaleY = exitScale
                },
            contentAlignment = Alignment.Center
        ) {
            // EMOJIS BURST LAYER (Omnidirectional 360-degree burst)
            val burstProgress = (progress / 0.28f).coerceIn(0f, 1f)
            val burstEase = 1f - (1f - burstProgress) * (1f - burstProgress) * (1f - burstProgress)

            particles.forEach { p ->
                val currentDistance = (p.targetDistance * burstEase) + (exitProgress * 25f)
                val transX = cos(p.angleRad) * currentDistance
                val transY = sin(p.angleRad) * currentDistance
                val currentScale = if (burstProgress < 0.25f) {
                    (burstProgress / 0.25f) * 1.15f
                } else {
                    1.0f - (exitProgress * 0.35f)
                }
                val rot = p.spinAngle * burstEase

                Box(
                    modifier = Modifier
                        .size(p.sizeDp.dp)
                        .graphicsLayer {
                            translationX = transX
                            translationY = transY
                            scaleX = currentScale
                            scaleY = currentScale
                            rotationZ = rot
                            alpha = smoothExitAlpha
                        }
                ) {
                    Apple3DGlossyEmojiCanvas(
                        kind = p.emojiKind,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // COLOR PALETTE FOR CURRENT ACTION
            val (accentGlow, rimGradient) = remember(celebration.type) {
                when (celebration.type) {
                    CelebrationActionType.EXPENSE_SAVED -> Pair(
                        GoldPrimary,
                        Brush.sweepGradient(listOf(GoldLight, GoldPrimary, Color.White, GoldDark, GoldLight))
                    )
                    CelebrationActionType.UDHAAR_RECORDED -> Pair(
                        OrangeWarning,
                        Brush.sweepGradient(listOf(Color(0xFFFDBA74), OrangeWarning, Color.White, GoldPrimary, Color(0xFFFDBA74)))
                    )
                    CelebrationActionType.UDHAAR_SETTLED -> Pair(
                        EmeraldCash,
                        Brush.sweepGradient(listOf(Color(0xFF34D399), EmeraldCash, Color.White, Color(0xFF00B894), Color(0xFF34D399)))
                    )
                    CelebrationActionType.POT_DEPOSIT, CelebrationActionType.POT_CREATED -> Pair(
                        Color(0xFF38EF7D),
                        Brush.sweepGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color.White, Color(0xFF11998E)))
                    )
                    CelebrationActionType.EXPENSE_DELETED -> Pair(
                        Color(0xFFFF7675),
                        Brush.sweepGradient(listOf(Color(0xFFD63031), Color(0xFFFF7675), Color.White, Color(0xFFD63031)))
                    )
                    CelebrationActionType.SYNC_SUCCESS -> Pair(
                        Color(0xFF00CEC9),
                        Brush.sweepGradient(listOf(Color(0xFF0984E3), Color(0xFF00CEC9), Color.White, Color(0xFF0984E3)))
                    )
                    CelebrationActionType.SETTINGS_SAVED -> Pair(
                        GoldPrimary,
                        Brush.sweepGradient(listOf(GoldLight, GoldPrimary, Color.White, GoldLight))
                    )
                }
            }

            // SUBTLE CONCENTRIC GLASS RIPPLES (Expanding outward from behind the badge)
            if (ripple1Progress.value in 0.01f..0.99f) {
                val r1 = ripple1Progress.value
                val r1Scale = 1.0f + (r1 * 0.85f)
                val r1Alpha = (1f - r1 * r1) * 0.50f * smoothExitAlpha
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            scaleX = r1Scale
                            scaleY = r1Scale
                            alpha = r1Alpha
                        }
                        .border(
                            1.5.dp,
                            Brush.radialGradient(
                                colors = listOf(
                                    accentGlow.copy(alpha = 0.75f),
                                    accentGlow.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentGlow.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }

            if (ripple2Progress.value in 0.01f..0.99f) {
                val r2 = ripple2Progress.value
                val r2Scale = 1.0f + (r2 * 1.30f)
                val r2Alpha = (1f - r2 * r2) * 0.32f * smoothExitAlpha
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            scaleX = r2Scale
                            scaleY = r2Scale
                            alpha = r2Alpha
                        }
                        .border(
                            1.2.dp,
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    accentGlow.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }

            // CENTERPIECE: Glass Jewel Badge + Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = jewelScale.value
                        scaleY = jewelScale.value
                    }
                    .padding(horizontal = 24.dp)
            ) {
                // Circular Jewel Emblem
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .border(2.dp, rimGradient, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xF0182236),
                                    Color(0xFA090D18)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft inner rim light reflection
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.5.dp)
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        accentGlow.copy(alpha = 0.35f)
                                    )
                                ),
                                CircleShape
                            )
                    )

                    // Ambient core glow pulse
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentGlow.copy(alpha = 0.30f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // CENTER ICON: Animated Drawing Tick for Check Actions, or Crisp Vector Icons
                    val isCheckAction = celebration.type == CelebrationActionType.EXPENSE_SAVED ||
                            celebration.type == CelebrationActionType.SETTINGS_SAVED ||
                            celebration.type == CelebrationActionType.UDHAAR_SETTLED

                    if (isCheckAction) {
                        AnimatedTickMark(
                            progress = tickProgress.value,
                            color = if (celebration.type == CelebrationActionType.UDHAAR_SETTLED) EmeraldCash else GoldLight,
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer {
                                    scaleX = tickBounce.value
                                    scaleY = tickBounce.value
                                }
                        )
                    } else {
                        val iconVector = when (celebration.type) {
                            CelebrationActionType.UDHAAR_RECORDED -> Icons.Default.SwapHoriz
                            CelebrationActionType.POT_DEPOSIT, CelebrationActionType.POT_CREATED -> Icons.Default.Savings
                            CelebrationActionType.EXPENSE_DELETED -> Icons.Default.DeleteSweep
                            CelebrationActionType.SYNC_SUCCESS -> Icons.Default.CloudDone
                            else -> Icons.Default.Check
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = "Action",
                            tint = if (celebration.type == CelebrationActionType.EXPENSE_DELETED) Color(0xFFFF7675) else GoldLight,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-contrast Frosted Glass Pill Badge for Title & Subtitle
                Surface(
                    color = Color(0xF20B101D),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, rimGradient),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = celebration.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.3.sp
                        )

                        if (!celebration.subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = celebration.subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentGlow
                            )
                        } else if (celebration.amount != null && celebration.amount > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${celebration.amount.toInt()} Note Ho Gaya",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldCash
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Drawing Tick Mark (Checkmark) using Compose PathMeasure.
 * Guarantees a seamless, uninterrupted, single continuous stroke around the corner
 * with StrokeCap.Round and StrokeJoin.Round plus soft specular glow!
 */
@Composable
private fun AnimatedTickMark(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 6.5.dp.toPx()

        // Checkmark anchor points
        val p1 = Offset(w * 0.22f, h * 0.52f) // Left start
        val p2 = Offset(w * 0.43f, h * 0.73f) // Vertex / bottom turn
        val p3 = Offset(w * 0.80f, h * 0.28f) // Top-right tip

        if (progress > 0.001f) {
            val fullPath = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(fullPath, false)
            val partialPath = Path()
            pathMeasure.getSegment(0f, pathMeasure.length * progress.coerceIn(0f, 1f), partialPath, true)

            // 1. Soft glowing background path for radiant illumination
            drawPath(
                path = partialPath,
                color = color.copy(alpha = 0.30f),
                style = Stroke(
                    width = strokeWidth + 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Crisp, solid foreground tick stroke with rounded caps and corner join
            drawPath(
                path = partialPath,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Renders authentic Apple-style 3D Glossy Emojis using high-performance Canvas.
 * Guaranteed 3D volumetric glass look on all Android devices (does NOT fall back to flat Android emojis).
 */
@Composable
private fun Apple3DGlossyEmojiCanvas(
    kind: GlossyEmojiKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f

        when (kind) {
            GlossyEmojiKind.HONEY_PARTY_FACE -> {
                drawAppleGlossySphere(cx, cy, radius * 0.9f)
                val eyeY = cy - radius * 0.08f
                val eyeOffset = radius * 0.32f
                drawCircle(Color(0xFF2D1800), radius * 0.12f, Offset(cx - eyeOffset, eyeY))
                drawCircle(Color.White, radius * 0.045f, Offset(cx - eyeOffset - 1.5f, eyeY - 1.5f))
                drawCircle(Color(0xFF2D1800), radius * 0.12f, Offset(cx + eyeOffset, eyeY))
                drawCircle(Color.White, radius * 0.045f, Offset(cx + eyeOffset - 1.5f, eyeY - 1.5f))

                drawCircle(Color(0x66FF4757), radius * 0.18f, Offset(cx - eyeOffset * 1.1f, eyeY + radius * 0.22f))
                drawCircle(Color(0x66FF4757), radius * 0.18f, Offset(cx + eyeOffset * 1.1f, eyeY + radius * 0.22f))

                drawArc(
                    color = Color(0xFF7A1D00),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - radius * 0.28f, cy),
                    size = Size(radius * 0.56f, radius * 0.44f)
                )
                val hatPath = Path().apply {
                    moveTo(cx - radius * 0.4f, cy - radius * 0.55f)
                    lineTo(cx - radius * 0.85f, cy - radius * 1.15f)
                    lineTo(cx + radius * 0.05f, cy - radius * 0.85f)
                    close()
                }
                drawPath(
                    hatPath,
                    Brush.linearGradient(
                        listOf(Color(0xFFFF3838), Color(0xFFFF9F1A), Color(0xFF2ED573))
                    )
                )
                drawCircle(Color(0xFFFFFA65), radius * 0.12f, Offset(cx - radius * 0.85f, cy - radius * 1.15f))
            }

            GlossyEmojiKind.GOLD_COIN_3D -> {
                drawCircle(Color(0x55000000), radius * 0.88f, Offset(cx + 2f, cy + 3f))
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEAA7), Color(0xFFF39C12), Color(0xFFB7791F)),
                        center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                        radius = radius * 0.9f
                    ),
                    radius * 0.88f,
                    Offset(cx, cy)
                )
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF7D6), Color(0xFFF1C40F), Color(0xFF996515)),
                        center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
                        radius = radius * 0.65f
                    ),
                    radius * 0.68f,
                    Offset(cx, cy)
                )
                drawLine(Color(0xFF5A3C00), Offset(cx - radius * 0.30f, cy - radius * 0.32f), Offset(cx + radius * 0.30f, cy - radius * 0.32f), strokeWidth = radius * 0.12f, cap = StrokeCap.Round)
                drawLine(Color(0xFF5A3C00), Offset(cx - radius * 0.30f, cy - radius * 0.12f), Offset(cx + radius * 0.20f, cy - radius * 0.12f), strokeWidth = radius * 0.10f, cap = StrokeCap.Round)
                drawLine(Color(0xFF5A3C00), Offset(cx - radius * 0.10f, cy - radius * 0.32f), Offset(cx - radius * 0.10f, cy + radius * 0.25f), strokeWidth = radius * 0.12f, cap = StrokeCap.Round)
                drawLine(Color(0xFF5A3C00), Offset(cx - radius * 0.05f, cy), Offset(cx + radius * 0.25f, cy + radius * 0.35f), strokeWidth = radius * 0.12f, cap = StrokeCap.Round)
                drawArc(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.75f, cy - radius * 0.75f),
                    size = Size(radius * 1.5f, radius * 1.5f),
                    style = Stroke(width = radius * 0.16f)
                )
            }

            GlossyEmojiKind.VOLUMETRIC_MONEY_WINGS -> {
                drawOval(
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xCCFFFFFF), Color(0x33B2BEC3)),
                        center = Offset(cx - radius * 0.6f, cy - radius * 0.4f),
                        radius = radius * 0.5f
                    ),
                    topLeft = Offset(cx - radius * 0.95f, cy - radius * 0.6f),
                    size = Size(radius * 0.65f, radius * 0.45f)
                )
                drawOval(
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xCCFFFFFF), Color(0x33B2BEC3)),
                        center = Offset(cx + radius * 0.6f, cy - radius * 0.4f),
                        radius = radius * 0.5f
                    ),
                    topLeft = Offset(cx + radius * 0.30f, cy - radius * 0.6f),
                    size = Size(radius * 0.65f, radius * 0.45f)
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF55EFC4), Color(0xFF00B894), Color(0xFF006266))),
                    topLeft = Offset(cx - radius * 0.55f, cy - radius * 0.25f),
                    size = Size(radius * 1.1f, radius * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.15f, radius * 0.15f)
                )
                drawCircle(Color(0x55FFFFFF), radius * 0.20f, Offset(cx, cy + radius * 0.05f))
            }

            GlossyEmojiKind.SPARKLE_STAR_3D -> {
                val starPath = Path().apply {
                    moveTo(cx, cy - radius * 0.95f)
                    quadraticTo(cx, cy, cx + radius * 0.95f, cy)
                    quadraticTo(cx, cy, cx, cy + radius * 0.95f)
                    quadraticTo(cx, cy, cx - radius * 0.95f, cy)
                    quadraticTo(cx, cy, cx, cy - radius * 0.95f)
                    close()
                }
                drawPath(
                    starPath,
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFFFFA65), Color(0xFFFFA502)),
                        center = Offset(cx, cy),
                        radius = radius * 0.8f
                    )
                )
                drawCircle(Color.White, radius * 0.22f, Offset(cx, cy))
            }

            GlossyEmojiKind.DIAMOND_GEM_3D -> {
                val gemPath = Path().apply {
                    moveTo(cx - radius * 0.7f, cy - radius * 0.3f)
                    lineTo(cx - radius * 0.35f, cy - radius * 0.75f)
                    lineTo(cx + radius * 0.35f, cy - radius * 0.75f)
                    lineTo(cx + radius * 0.7f, cy - radius * 0.3f)
                    lineTo(cx, cy + radius * 0.85f)
                    close()
                }
                drawPath(
                    gemPath,
                    Brush.verticalGradient(
                        listOf(Color(0xFFE0FFFF), Color(0xFF00CEC9), Color(0xFF0984E3))
                    )
                )
                drawLine(Color.White, Offset(cx - radius * 0.35f, cy - radius * 0.75f), Offset(cx, cy + radius * 0.85f), strokeWidth = 2f)
                drawLine(Color.White, Offset(cx + radius * 0.35f, cy - radius * 0.75f), Offset(cx, cy + radius * 0.85f), strokeWidth = 2f)
            }

            GlossyEmojiKind.FIRE_FLAME_3D -> {
                val flamePath = Path().apply {
                    moveTo(cx, cy - radius * 0.95f)
                    cubicTo(cx + radius * 0.8f, cy - radius * 0.1f, cx + radius * 0.7f, cy + radius * 0.8f, cx, cy + radius * 0.9f)
                    cubicTo(cx - radius * 0.7f, cy + radius * 0.8f, cx - radius * 0.8f, cy - radius * 0.1f, cx, cy - radius * 0.95f)
                    close()
                }
                drawPath(
                    flamePath,
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFA65), Color(0xFFFF4757), Color(0xFFB33939)),
                        center = Offset(cx, cy + radius * 0.2f),
                        radius = radius * 0.9f
                    )
                )
            }

            GlossyEmojiKind.RUBY_HEART_3D -> {
                val heartPath = Path().apply {
                    moveTo(cx, cy + radius * 0.8f)
                    cubicTo(cx - radius * 0.9f, cy + radius * 0.1f, cx - radius * 0.9f, cy - radius * 0.7f, cx - radius * 0.45f, cy - radius * 0.7f)
                    cubicTo(cx - radius * 0.15f, cy - radius * 0.7f, cx, cy - radius * 0.4f, cx, cy - radius * 0.25f)
                    cubicTo(cx, cy - radius * 0.4f, cx + radius * 0.15f, cy - radius * 0.7f, cx + radius * 0.45f, cy - radius * 0.7f)
                    cubicTo(cx + radius * 0.9f, cy - radius * 0.7f, cx + radius * 0.9f, cy + radius * 0.1f, cx, cy + radius * 0.8f)
                    close()
                }
                drawPath(
                    heartPath,
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF7675), Color(0xFFD63031), Color(0xFF570000)),
                        center = Offset(cx - radius * 0.2f, cy - radius * 0.3f),
                        radius = radius * 0.9f
                    )
                )
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)),
                    topLeft = Offset(cx - radius * 0.6f, cy - radius * 0.6f),
                    size = Size(radius * 0.35f, radius * 0.25f)
                )
            }

            GlossyEmojiKind.COOL_SHADES_FACE -> {
                drawAppleGlossySphere(cx, cy, radius * 0.9f)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF2D3436), Color(0xFF1E272E))),
                    topLeft = Offset(cx - radius * 0.75f, cy - radius * 0.3f),
                    size = Size(radius * 1.5f, radius * 0.45f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.12f, radius * 0.12f)
                )
                drawLine(
                    Color.White.copy(alpha = 0.75f),
                    Offset(cx - radius * 0.55f, cy - radius * 0.25f),
                    Offset(cx - radius * 0.15f, cy + radius * 0.05f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color.White.copy(alpha = 0.75f),
                    Offset(cx + radius * 0.15f, cy - radius * 0.25f),
                    Offset(cx + radius * 0.55f, cy + radius * 0.05f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = Color(0xFF7A1D00),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.35f, cy + radius * 0.15f),
                    size = Size(radius * 0.7f, radius * 0.35f),
                    style = Stroke(width = radius * 0.1f, cap = StrokeCap.Round)
                )
            }

            GlossyEmojiKind.POOF_SMOKE_BUBBLE -> {
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.9f), Color(0x99DFE6E9), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius * 0.75f
                    ),
                    radius * 0.75f,
                    Offset(cx, cy)
                )
                drawCircle(Color.White.copy(alpha = 0.85f), radius * 0.4f, Offset(cx - radius * 0.35f, cy - radius * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.85f), radius * 0.45f, Offset(cx + radius * 0.3f, cy - radius * 0.15f))
                drawCircle(Color.White.copy(alpha = 0.85f), radius * 0.38f, Offset(cx, cy + radius * 0.3f))
            }

            GlossyEmojiKind.ROCKET_3D -> {
                val rocketPath = Path().apply {
                    moveTo(cx, cy - radius * 0.9f)
                    cubicTo(cx + radius * 0.5f, cy - radius * 0.4f, cx + radius * 0.45f, cy + radius * 0.4f, cx, cy + radius * 0.65f)
                    cubicTo(cx - radius * 0.45f, cy + radius * 0.4f, cx - radius * 0.5f, cy - radius * 0.4f, cx, cy - radius * 0.9f)
                    close()
                }
                drawPath(
                    rocketPath,
                    Brush.linearGradient(
                        listOf(Color.White, Color(0xFFDFE6E9), Color(0xFFB2BEC3))
                    )
                )
                drawCircle(Color(0xFF0984E3), radius * 0.16f, Offset(cx, cy - radius * 0.15f))
                drawCircle(Color.White, radius * 0.05f, Offset(cx - 2f, cy - radius * 0.15f - 2f))
                drawCircle(Color(0xFFFF7675), radius * 0.20f, Offset(cx, cy + radius * 0.75f))
                drawCircle(Color(0xFFFFFA65), radius * 0.10f, Offset(cx, cy + radius * 0.70f))
            }
        }
    }
}

/**
 * Draws the signature Apple-style volumetric honey-yellow glass sphere with specular highlight.
 */
private fun DrawScope.drawAppleGlossySphere(cx: Float, cy: Float, r: Float) {
    drawCircle(
        Color(0x35000000),
        r,
        Offset(cx, cy + r * 0.12f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFEA79),
                Color(0xFFFFD32A),
                Color(0xFFFFA801),
                Color(0xFFE58E26),
                Color(0xFF9E4E00)
            ),
            center = Offset(cx - r * 0.25f, cy - r * 0.25f),
            radius = r * 1.15f
        ),
        radius = r,
        center = Offset(cx, cy)
    )

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.35f),
                Color.Transparent
            )
        ),
        topLeft = Offset(cx - r * 0.65f, cy - r * 0.85f),
        size = Size(r * 1.3f, r * 0.75f)
    )

    drawArc(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x66FFEAA7))
        ),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.9f, cy - r * 0.9f),
        size = Size(r * 1.8f, r * 1.8f),
        style = Stroke(width = r * 0.12f)
    )
}
