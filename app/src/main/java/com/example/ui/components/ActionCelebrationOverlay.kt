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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
 * Subtle Tactile Haptic Helper:
 * Fast, crisp micro-vibrations for entry click and checkmark lock.
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
                    vibrator.vibrate(VibrationEffect.createOneShot(12, 45))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
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
                    vibrator.vibrate(VibrationEffect.createOneShot(8, 35))
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
 * Native Android PCM Audio Synthesizer:
 * Warm, soothing, low-pitched acoustic chime (Apple Pay / Marimba style).
 * Capped under 660 Hz with soft attack and warm decay — completely eliminated harsh high pitch and screeching overtones!
 */
object AudioSynthesizer {
    private const val SAMPLE_RATE = 44100

    suspend fun playForType(type: CelebrationActionType) = withContext(Dispatchers.Default) {
        when (type) {
            CelebrationActionType.EXPENSE_SAVED,
            CelebrationActionType.POT_DEPOSIT,
            CelebrationActionType.POT_CREATED,
            CelebrationActionType.SETTINGS_SAVED -> {
                // Warm, pleasant 2-tone Apple Pay style acoustic chime: 440Hz (A4) -> 587.33Hz (D5) -> 659.25Hz (E5)
                playWarmAcousticChime()
            }
            CelebrationActionType.UDHAAR_RECORDED,
            CelebrationActionType.UDHAAR_SETTLED -> {
                // Hyper-satisfying Udhaar audio: Soft catch thump (120Hz -> 55Hz) + coin clink + golden chord chime (G5->B5->D6->G6)
                playUdhaarHandoverAudio()
            }
            CelebrationActionType.EXPENSE_DELETED -> {
                // Razor slice (880Hz -> 160Hz over 80ms) + Snap thud (160Hz -> 38Hz over 160ms) + Vaporize echo (392Hz -> 311Hz)
                playCyberDeleteAudio()
            }
            CelebrationActionType.SYNC_SUCCESS -> {
                playWarmArpeggio(doubleArrayOf(349.23, 440.0, 523.25), noteDurationMs = 70, decay = 5.2)
            }
        }
    }

    private fun playWarmAcousticChime() {
        try {
            // Warm, mellow acoustic frequencies (G4: 392Hz, C5: 523Hz, E5: 659Hz)
            // No ear-piercing high frequencies (completely avoids anything above 660Hz)
            val warmFrequencies = doubleArrayOf(392.0, 523.25, 659.25)
            val noteDurationMs = 65
            val noteSamples = (SAMPLE_RATE * (noteDurationMs / 1000.0)).toInt()
            val totalSamples = noteSamples * warmFrequencies.size + (SAMPLE_RATE * 0.28).toInt()
            val buffer = ShortArray(totalSamples)

            for (n in warmFrequencies.indices) {
                val freq = warmFrequencies[n]
                val startIdx = n * noteSamples
                val maxLen = totalSamples - startIdx
                for (i in 0 until maxLen) {
                    val t = i.toDouble() / SAMPLE_RATE
                    // Soft 10ms sinusoidal attack to eliminate any initial click/harshness
                    val attack = if (t < 0.010) sin((t / 0.010) * (PI / 2.0)) else 1.0
                    // Warm, gentle wooden decay
                    val envelope = attack * exp(-5.0 * t)
                    // Pure, velvety fundamental with gentle warmth (no harsh high harmonics)
                    val fundamental = sin(2.0 * PI * freq * t)
                    val warmSecondHarmonic = sin(4.0 * PI * freq * t) * 0.10
                    // Comfortable, gentle volume (0.18 max amplitude)
                    val sample = ((fundamental + warmSecondHarmonic) * envelope * Short.MAX_VALUE * 0.18).toInt()
                    val curr = buffer[startIdx + i]
                    val mixed = (curr + sample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    buffer[startIdx + i] = mixed.toShort()
                }
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playWarmArpeggio(frequencies: DoubleArray, noteDurationMs: Int, decay: Double) {
        try {
            val totalNotes = frequencies.size
            val noteSamples = (SAMPLE_RATE * (noteDurationMs / 1000.0)).toInt()
            val totalSamples = noteSamples * totalNotes + (SAMPLE_RATE * 0.20).toInt()
            val buffer = ShortArray(totalSamples)

            for (n in frequencies.indices) {
                val freq = frequencies[n]
                val startIdx = n * noteSamples
                val maxLen = totalSamples - startIdx
                for (i in 0 until maxLen) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val attack = if (t < 0.008) sin((t / 0.008) * (PI / 2.0)) else 1.0
                    val envelope = attack * exp(-decay * t)
                    val sine = sin(2.0 * PI * freq * t)
                    val warmSecond = sin(4.0 * PI * freq * t) * 0.08
                    val sampleValue = ((sine + warmSecond) * envelope * Short.MAX_VALUE * 0.18).toInt()
                    val curr = buffer[startIdx + i]
                    val mixed = (curr + sampleValue).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    buffer[startIdx + i] = mixed.toShort()
                }
            }
            playPcm(buffer)
        } catch (_: Exception) {}
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
                val sampleValue = (sin(phase) * envelope * Short.MAX_VALUE * 0.38).toInt()
                buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playCyberDeleteAudio() {
        try {
            // Part 1: Razor slice (80ms): Triangle wave ramp 880Hz -> 160Hz
            val sliceSamples = (SAMPLE_RATE * 0.080).toInt()
            // Part 2: Snap thud (160ms): Sine wave 160Hz -> 38Hz
            val snapSamples = (SAMPLE_RATE * 0.160).toInt()
            // Part 3: Vaporize echo (180ms): G4 (392Hz) -> Eb4 (311Hz)
            val echoSamples = (SAMPLE_RATE * 0.180).toInt()
            val totalSamples = sliceSamples + snapSamples + echoSamples
            val buffer = ShortArray(totalSamples)

            // 1. Razor slice
            var slicePhase = 0.0
            for (i in 0 until sliceSamples) {
                val p = i.toDouble() / sliceSamples
                val freq = 880.0 + (160.0 - 880.0) * p
                slicePhase += 2.0 * PI * freq / SAMPLE_RATE
                val normalizedPhase = (slicePhase % (2.0 * PI)) / (2.0 * PI)
                val tri = if (normalizedPhase < 0.5) (4.0 * normalizedPhase - 1.0) else (3.0 - 4.0 * normalizedPhase)
                val envelope = (1.0 - p) * 0.20
                buffer[i] = (tri * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // 2. Snap thud
            var snapPhase = 0.0
            for (i in 0 until snapSamples) {
                val p = i.toDouble() / snapSamples
                val freq = 160.0 + (38.0 - 160.0) * (p * p)
                snapPhase += 2.0 * PI * freq / SAMPLE_RATE
                val envelope = exp(-4.5 * p) * 0.26
                val sample = (sin(snapPhase) * envelope * Short.MAX_VALUE).toInt()
                val idx = sliceSamples + i
                if (idx < totalSamples) {
                    val curr = buffer[idx]
                    buffer[idx] = (curr + sample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            // 3. Vaporize echo
            var echoPhase = 0.0
            for (i in 0 until echoSamples) {
                val p = i.toDouble() / echoSamples
                val freq = 392.0 + (311.0 - 392.0) * p
                echoPhase += 2.0 * PI * freq / SAMPLE_RATE
                val envelope = exp(-5.0 * p) * 0.14
                val sample = (sin(echoPhase) * envelope * Short.MAX_VALUE).toInt()
                val idx = sliceSamples + snapSamples + i
                if (idx < totalSamples) {
                    buffer[idx] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playUdhaarHandoverAudio() {
        try {
            // 1. Soft catch thump: Mellow sub-bass sine drop 120Hz -> 55Hz (100ms)
            val thumpSamples = (SAMPLE_RATE * 0.100).toInt()
            // 2. Coin clink: Two crisp yet warm sine pings at 1175Hz (D6) and 1568Hz (G6) (80ms)
            val clinkSamples = (SAMPLE_RATE * 0.080).toInt()
            // 3. Golden chord chime: Warm harmonic bells G5 (784Hz) -> B5 (988Hz) -> D6 (1175Hz) -> G6 (1568Hz)
            val bellFrequencies = doubleArrayOf(783.99, 987.77, 1174.66, 1567.98)
            val bellNoteDurationMs = 60
            val bellNoteSamples = (SAMPLE_RATE * (bellNoteDurationMs / 1000.0)).toInt()
            val bellTotalSamples = bellNoteSamples * bellFrequencies.size + (SAMPLE_RATE * 0.25).toInt()

            val totalSamples = maxOf(thumpSamples + clinkSamples + bellTotalSamples, (SAMPLE_RATE * 0.55).toInt())
            val buffer = ShortArray(totalSamples)

            // 1. Soft catch thump (0ms to 100ms)
            var thumpPhase = 0.0
            for (i in 0 until thumpSamples) {
                val p = i.toDouble() / thumpSamples
                val freq = 120.0 + (55.0 - 120.0) * (p * p)
                thumpPhase += 2.0 * PI * freq / SAMPLE_RATE
                val envelope = exp(-4.5 * p) * 0.22
                val sample = (sin(thumpPhase) * envelope * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // 2. Coin clink (starts at 110ms)
            val clinkStart = (SAMPLE_RATE * 0.110).toInt()
            for (i in 0 until clinkSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-32.0 * t) * 0.12
                val ping1 = sin(2.0 * PI * 1174.66 * t) // D6
                val ping2 = sin(2.0 * PI * 1567.98 * t) // G6
                val sample = ((ping1 + ping2) * env * Short.MAX_VALUE).toInt()
                val idx = clinkStart + i
                if (idx < totalSamples) {
                    val curr = buffer[idx]
                    buffer[idx] = (curr + sample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            // 3. Golden chord chime (starts around 170ms)
            val chimeStart = (SAMPLE_RATE * 0.170).toInt()
            for (n in bellFrequencies.indices) {
                val freq = bellFrequencies[n]
                val noteOffset = chimeStart + n * bellNoteSamples
                val maxLen = totalSamples - noteOffset
                for (i in 0 until maxLen) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val attack = if (t < 0.008) sin((t / 0.008) * (PI / 2.0)) else 1.0
                    val envelope = attack * exp(-5.5 * t)
                    val fundamental = sin(2.0 * PI * freq * t)
                    val warmSecond = sin(4.0 * PI * freq * t) * 0.08
                    val sampleValue = ((fundamental + warmSecond) * envelope * Short.MAX_VALUE * 0.16).toInt()
                    val idx = noteOffset + i
                    if (idx < totalSamples) {
                        val curr = buffer[idx]
                        buffer[idx] = (curr + sampleValue).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
            }

            playPcm(buffer)
        } catch (_: Exception) {}
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
                    Thread.sleep((buffer.size * 1000L / SAMPLE_RATE) + 60)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }
}

/**
 * 4 Starburst Twinkle Particles from exact SVG path spec:
 * path: "M12 0L14.59 9.41L24 12L14.59 14.59L12 24L9.41 14.59L0 12L9.41 9.41L12 0Z"
 */
private data class StarburstParticle(
    val offsetX: Float,
    val offsetY: Float,
    val delayMs: Int,
    val color: Color,
    val sizeDp: Float
)

/**
 * 6 Disintegrating Ember Sparks for Cyber-Clean Delete animation:
 * Exact coordinates:
 * (x: 48, y: -42, rot: 45°), (x: -52, y: -38, rot: -60°), (x: 56, y: 32, rot: 120°),
 * (x: -48, y: 36, rot: -135°), (x: 0, y: -58, rot: 90°), (x: -28, y: -50, rot: -30°).
 */
private data class EmberSparkParticle(
    val offsetX: Float,
    val offsetY: Float,
    val rotDeg: Float,
    val color: Color
)

/**
 * Money-Related 3D Apple Emojis for Background Burst.
 */
private enum class MoneyEmojiKind {
    APPLE_GOLD_COIN,       // 🪙 3D Apple Gold Coin with Rupee emboss
    APPLE_FLYING_CASH,     // 💸 3D Apple Flying Cash with Wings
    APPLE_MONEY_BAG,       // 💰 3D Apple Money Bag with golden drawstring
    APPLE_DIAMOND_GEM      // 💎 3D Apple Crystalline Gem
}

private data class MoneyEmojiParticle(
    val kind: MoneyEmojiKind,
    val angleRad: Float,
    val orbitDistanceDp: Float,
    val spinDeg: Float,
    val sizeDp: Float
)

/**
 * Master Ultimate Quick Add Celebration Overlay:
 * Strictly follows the exact architectural and visual specifications:
 * 1. Fullscreen dark backdrop vignette (bg-black/75, fades in over 0.25s, unmounts at 1.6s).
 * 2. Expanding Shockwave Halo Ring (w-32 h-32, border-2 emerald-400/40, scale 0.85 -> 1.6, opacity 0.8 -> 0).
 * 3. 4 Starburst Twinkle Particles around center medallion (amber-300, emerald-300, teal-200).
 * 4. Money-Related 3D Apple Emoji Burst in the background (Gold coin, cash with wings, money bag, diamond gem).
 * 5. Glossy Emerald Medallion (spring recoil scale 0.4 -> 1.0, metallic emerald/teal/amber border, dark emerald core, curved specular glass reflection rim).
 * 6. In-Badge Vector Drawing: static guide circle + animated drawing outer ring (rotate -90 -> 0, pathLength 0 -> 1 over 0.42s) + crisp white checkmark drawing (0 -> 1 over 0.35s).
 * 7. Synthesized Chime (170Hz -> 45Hz click + ascending crystal bells 659, 987, 1318, 1975Hz).
 * 8. Butter-smooth tap-to-dismiss (0.30s rapid scale & fade).
 */
@Composable
fun ActionCelebrationOverlay(
    celebration: CelebrationData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Master presentation state (single source of truth for smooth transitions)
    val masterAlpha = remember { Animatable(0f) }
    val masterScale = remember { Animatable(0.4f) }
    val masterSlideY = remember { Animatable(0f) }

    // Atomic dismissal state preventing duplicate triggers or race conditions
    var isDismissing by remember { mutableStateOf(false) }

    // Robust Interrupt Handler:
    // Captures exact current visual coordinates and smoothly eases out in 240ms (cleanly sub-0.5s)
    // with ZERO jarring cuts, zero freezing, and zero pops!
    val triggerInterrupt: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                launch {
                    masterAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    masterScale.animateTo(
                        targetValue = (masterScale.value * 0.90f).coerceAtLeast(0.3f),
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    masterSlideY.animateTo(
                        targetValue = masterSlideY.value - 16f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                }
                delay(245)
                onDismiss()
            }
        }
    }

    val isDeleteAction = celebration.type == CelebrationActionType.EXPENSE_DELETED
    val isUdhaarAction = celebration.type == CelebrationActionType.UDHAAR_RECORDED || celebration.type == CelebrationActionType.UDHAAR_SETTLED

    val totalHoldMs = if (isUdhaarAction) 1450L else 1250L
    val totalSeqMs = if (isUdhaarAction) 1800 else 1600

    // 1. Lifecycle & Natural Exit Handler (1600ms/1800ms total sequence)
    LaunchedEffect(celebration.id) {
        SubtleHapticHelper.playSubtleEntrance(context, haptic)
        AudioSynthesizer.playForType(celebration.type)

        // Smooth 200ms entrance fade-in
        launch {
            masterAlpha.animateTo(1f, tween(durationMillis = 200, easing = FastOutSlowInEasing))
        }

        // Spring recoil entrance: scale 0.4 -> 1.0 (stiffness: 300, damping: 22)
        launch {
            masterScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f, // damping: 22
                    stiffness = 300f      // stiffness: 300
                )
            )
        }

        // Sequence hold: 1450ms for Udhaar, 1250ms for regular
        delay(totalHoldMs)

        // Natural smooth exit if user hasn't tapped yet
        if (!isDismissing) {
            isDismissing = true
            launch {
                masterAlpha.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
            }
            launch {
                masterScale.animateTo(0.9f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
            }
            launch {
                masterSlideY.animateTo(-20f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
            }
            delay(355)
            onDismiss()
        }
    }

    // Progress for internal effects (halo, checkmark, drawing ring)
    val sequenceProgress = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        sequenceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalSeqMs, easing = LinearEasing)
        )
    }

    // Sleek Floating Amount Pill animation (delay: 0.28s / 280ms, spring stiffness: 300, damping: 22)
    val amountPillProgress = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        if (isUdhaarAction) {
            delay(280)
            amountPillProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f, // damping: 22
                    stiffness = 300f      // stiffness: 300
                )
            )
        }
    }

    // Center Jewel Rotation on Spring Entry (-8deg -> 0deg for delete action)
    val jewelRotation = remember { Animatable(if (isDeleteAction) -8f else 0f) }
    LaunchedEffect(celebration.id) {
        if (isDeleteAction) {
            jewelRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.72f, // damping: 22
                    stiffness = 320f      // stiffness: 320
                )
            )
        }
    }

    // Double Shockwave Rings for Delete Action
    val deleteShockwave1 = remember { Animatable(0f) }
    val deleteShockwave2 = remember { Animatable(0f) }

    LaunchedEffect(celebration.id) {
        if (isDeleteAction) {
            launch {
                deleteShockwave1.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
            }
            launch {
                delay(100)
                deleteShockwave2.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            }
        }
    }

    // 6 Disintegrating Ember Sparks for Cyber-Clean Delete
    val emberSparks = remember {
        listOf(
            EmberSparkParticle(offsetX = 48f, offsetY = -42f, rotDeg = 45f, color = Color(0xFFF43F5E)),
            EmberSparkParticle(offsetX = -52f, offsetY = -38f, rotDeg = -60f, color = Color(0xFFFB923C)),
            EmberSparkParticle(offsetX = 56f, offsetY = 32f, rotDeg = 120f, color = Color(0xFFFBBF24)),
            EmberSparkParticle(offsetX = -48f, offsetY = 36f, rotDeg = -135f, color = Color(0xFFFDA4AF)),
            EmberSparkParticle(offsetX = 0f, offsetY = -58f, rotDeg = 90f, color = Color(0xFFF43F5E)),
            EmberSparkParticle(offsetX = -28f, offsetY = -50f, rotDeg = -30f, color = Color(0xFFFB923C))
        )
    }

    // Expanding Shockwave Halo Ring: scale 0.85 -> 1.6 over 1.1s easeOut (for regular celebrations)
    val haloProgress = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        if (!isDeleteAction) {
            haloProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }
    }

    // In-Badge Vector Drawing:
    // Outer Ring: rotate -90 -> 0, pathLength 0 -> 1 over 0.42s
    val ringDrawProgress = remember { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        ringDrawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }

    // Crisp White Checkmark: delay 0.2s, pathLength 0 -> 1 over 0.35s with snappy ease [0.16, 1, 0.3, 1]
    val checkmarkProgress = remember { Animatable(0f) }
    val checkmarkBounce = remember { Animatable(1f) }
    LaunchedEffect(celebration.id) {
        delay(200)
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 350,
                easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
            )
        )
        // Settling micro-haptic & gentle bounce
        SubtleHapticHelper.playSubtleTickComplete(context, haptic)
        checkmarkBounce.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
        checkmarkBounce.animateTo(1.0f)
    }

    // 4 Starburst Twinkle Particles (exact coordinates & colors from prompt)
    val starbursts = remember(isUdhaarAction) {
        if (isUdhaarAction) {
            listOf(
                StarburstParticle(offsetX = 55f, offsetY = -38f, delayMs = 100, color = Color(0xFFFDE047), sizeDp = 14f), // gold #fde047
                StarburstParticle(offsetX = -52f, offsetY = -32f, delayMs = 140, color = Color(0xFFF59E0B), sizeDp = 12f), // amber #f59e0b
                StarburstParticle(offsetX = 62f, offsetY = 34f, delayMs = 160, color = Color(0xFF6EE7B7), sizeDp = 11f),  // emerald #6ee7b7
                StarburstParticle(offsetX = -56f, offsetY = 40f, delayMs = 120, color = Color(0xFFFDE047), sizeDp = 13f)  // gold #fde047
            )
        } else {
            listOf(
                StarburstParticle(offsetX = 50f, offsetY = -36f, delayMs = 100, color = Color(0xFFFCD34D), sizeDp = 14f), // Star 1: amber-300
                StarburstParticle(offsetX = -48f, offsetY = -32f, delayMs = 140, color = Color(0xFF6EE7B7), sizeDp = 12f), // Star 2: emerald-300
                StarburstParticle(offsetX = 54f, offsetY = 32f, delayMs = 160, color = Color(0xFF99F6E4), sizeDp = 11f),  // Star 3: teal-200
                StarburstParticle(offsetX = -52f, offsetY = 38f, delayMs = 120, color = Color(0xFFFCD34D), sizeDp = 13f)  // Star 4: amber-300
            )
        }
    }

    // Money-Related 3D Apple Emoji Burst in the Background
    val moneyEmojis = remember {
        listOf(
            MoneyEmojiParticle(
                kind = MoneyEmojiKind.APPLE_GOLD_COIN,
                angleRad = -PI.toFloat() * 0.22f, // Top-Right (~ -40°)
                orbitDistanceDp = 74f,
                spinDeg = 14f,
                sizeDp = 30f
            ),
            MoneyEmojiParticle(
                kind = MoneyEmojiKind.APPLE_FLYING_CASH,
                angleRad = -PI.toFloat() * 0.78f, // Top-Left (~ -140°)
                orbitDistanceDp = 76f,
                spinDeg = -12f,
                sizeDp = 32f
            ),
            MoneyEmojiParticle(
                kind = MoneyEmojiKind.APPLE_MONEY_BAG,
                angleRad = PI.toFloat() * 0.12f,  // Mid-Right (~ +22°)
                orbitDistanceDp = 72f,
                spinDeg = 16f,
                sizeDp = 30f
            ),
            MoneyEmojiParticle(
                kind = MoneyEmojiKind.APPLE_DIAMOND_GEM,
                angleRad = PI.toFloat() * 0.88f,  // Mid-Left (~ +158°)
                orbitDistanceDp = 72f,
                spinDeg = -14f,
                sizeDp = 28f
            )
        )
    }

    val progress = sequenceProgress.value

    // Fullscreen Overlay with PointerEventPass.Initial touch interruption:
    // Captures user taps immediately on ACTION_DOWN anywhere on the screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(celebration.id) {
                awaitPointerEventScope {
                    while (!isDismissing) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            triggerInterrupt()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft dark vignette backdrop (bg-black/80 for delete, bg-black/75 for success)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(masterAlpha.value * (if (isDeleteAction) 0.80f else 0.75f))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x99000000), // center vignette
                            Color(if (isDeleteAction) 0xCC000000 else 0xBF000000)  // bg-black/80 for delete, bg-black/75 for success
                        )
                    )
                )
        )

        // Center Container
        Box(
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer {
                    alpha = masterAlpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            if (isDeleteAction) {
                // DELETE ANIMATION: DOUBLE SHOCKWAVE RINGS
                // Shockwave 1: w-32 h-32 (128dp), border-2 border-rose-500/60, scale 0.8 -> 1.85, opacity 0.9 -> 0 over 0.95s
                val s1 = deleteShockwave1.value
                if (s1 in 0.001f..0.999f) {
                    val scale1 = 0.8f + (1.85f - 0.8f) * s1
                    val a1 = ((1f - s1) * 0.90f * masterAlpha.value).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer {
                                scaleX = scale1
                                scaleY = scale1
                                alpha = a1
                            }
                            .border(2.dp, Color(0x99F43F5E), CircleShape)
                    )
                }

                // Shockwave 2: Staggered inner ring, border border-amber-500/40, scale 0.6 -> 1.4, opacity 0.7 -> 0 over 0.75s, delay 100ms
                val s2 = deleteShockwave2.value
                if (s2 in 0.001f..0.999f) {
                    val scale2 = 0.6f + (1.4f - 0.6f) * s2
                    val a2 = ((1f - s2) * 0.70f * masterAlpha.value).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer {
                                scaleX = scale2
                                scaleY = scale2
                                alpha = a2
                            }
                            .border(1.2.dp, Color(0x66F59E0B), CircleShape)
                    )
                }

                // 6 DISINTEGRATING EMBER SPARKS: Bursting outwards in 6 distinct angles
                val currentMs = (progress * 1600).toInt()
                emberSparks.forEach { ember ->
                    val p = (currentMs / 750f).coerceIn(0f, 1f)
                    if (p in 0.001f..0.999f) {
                        val sparkScale = if (p < 0.40f) (p / 0.40f) * 1.4f else ((1f - p) / 0.60f) * 1.4f
                        val sparkAlpha = (sin(p * PI)).toFloat() * masterAlpha.value
                        val transX = with(density) { (ember.offsetX * (0.8f + 0.2f * p)).dp.toPx() }
                        val transY = with(density) { (ember.offsetY * (0.8f + 0.2f * p)).dp.toPx() }

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer {
                                    translationX = transX
                                    translationY = transY
                                    scaleX = sparkScale
                                    scaleY = sparkScale
                                    rotationZ = ember.rotDeg * (0.8f + 0.2f * p)
                                    alpha = sparkAlpha
                                }
                        ) {
                            FourPointStarSvgCanvas(color = ember.color, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // CENTER CRIMSON JEWEL BADGE (PURE BADGE ONLY — NO TEXT LABEL / NO PILL CARD)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = masterScale.value
                            scaleY = masterScale.value
                            rotationZ = jewelRotation.value
                            translationY = masterSlideY.value
                        }
                        .padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE11D48), // rose-600
                                        Color(0xFFF43F5E), // rose-500
                                        Color(0xFFF59E0B)  // amber-500
                                    ),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            // Inner Chamber: Deep crimson core (from-[#2a0711] via-[#1a040b] to-[#0d0206])
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2A0711),
                                        Color(0xFF1A040B),
                                        Color(0xFF0D0206)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Specular Glass Highlight: Upper half gloss reflection
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 8.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)
                                )
                        )

                        // In-Badge Mechanical Shred & Cyber Trash Can SVG Animation
                        MechanicalShredCanSvgCanvas(
                            progress = sequenceProgress.value,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                }
            } else if (isUdhaarAction) {
                // UDHAAR CELEBRATION (Luxury Money Handover Overlay):
                // LAYER 1: GOLDEN HALO RING (w-32 h-32 / 128dp, border-2 border-amber-400/40, scale 0.85 -> 1.6, opacity 0.8 -> 0 over 1.1s)
                val hp = haloProgress.value
                if (hp in 0.001f..0.999f) {
                    val haloScale = 0.85f + (1.6f - 0.85f) * hp
                    val haloAlpha = ((1f - hp) * 0.80f * masterAlpha.value).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer {
                                scaleX = haloScale
                                scaleY = haloScale
                                alpha = haloAlpha
                            }
                            .border(
                                width = 2.dp,
                                color = Color(0x66F59E0B), // border-amber-400/40
                                shape = CircleShape
                            )
                    )
                }

                // LAYER 2: 4 CRISP SPARKLE STARS around badge (offsets: [55, -38], [-52, -32], [62, 34], [-56, 40])
                val currentMs = (progress * 1800).toInt()
                starbursts.forEach { star ->
                    val starElapsed = (currentMs - star.delayMs).coerceAtLeast(0)
                    if (starElapsed > 0 && starElapsed < 900) {
                        val p = starElapsed / 900f
                        val starScale = if (p < 0.25f) (p / 0.25f) * 1.2f else 1.2f - (0.4f * (p - 0.25f) / 0.75f)
                        val starAlpha = (1f - p * p) * masterAlpha.value

                        val transXPx = with(density) { (star.offsetX * (0.85f + 0.15f * p)).dp.toPx() }
                        val transYPx = with(density) { (star.offsetY * (0.85f + 0.15f * p)).dp.toPx() - 20.dp.toPx() }

                        Box(
                            modifier = Modifier
                                .size(star.sizeDp.dp)
                                .graphicsLayer {
                                    translationX = transXPx
                                    translationY = transYPx
                                    scaleX = starScale
                                    scaleY = starScale
                                    alpha = starAlpha
                                }
                        ) {
                            FourPointStarSvgCanvas(color = star.color, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // LAYER 3: THE CENTER JEWEL BADGE (Gold & Amber Medallion) + SLEEK FLOATING AMOUNT PILL
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = masterScale.value
                            scaleY = masterScale.value
                            translationY = masterSlideY.value
                        }
                        .padding(horizontal = 20.dp)
                ) {
                    // Outer Ring: Luxury Medallion (w-28 h-28 / 104dp) wrapped in metallic gradient
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF59E0B), // amber-500
                                        Color(0xFFFDE047), // yellow-300
                                        Color(0xFF34D399)  // emerald-400
                                    ),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            // Inner Capsule: Deep obsidian-amber core (from-[#241705] via-[#160d02] to-[#0b0601])
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF241705),
                                        Color(0xFF160D02),
                                        Color(0xFF0B0601)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Specular Glass Highlight: Upper half reflection rim (white/10 to transparent)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 8.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp)
                                )
                        )

                        // In-Badge SVG Hand & Money Flow Animation (ViewBox: 0 0 84 84)
                        UdhaarHandMoneyFlowSvgCanvas(
                            progress = sequenceProgress.value,
                            modifier = Modifier.size(82.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // LAYER 4: SLEEK FLOATING AMOUNT PILL BENEATH JEWEL BADGE
                    // Spring entry from y: 8, scale: 0.7, opacity: 0 to y: 0, scale: 1, opacity: 1 (delay 0.28s)
                    val pillP = amountPillProgress.value
                    val pillScale = 0.7f + 0.3f * pillP
                    val pillY = 8f * (1f - pillP)
                    val pillAlpha = pillP * masterAlpha.value

                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pillScale
                                scaleY = pillScale
                                translationY = pillY
                                alpha = pillAlpha
                            },
                        color = Color(0xF2160D02), // bg-[#160d02]/90
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xB3F59E0B), // amber-500/70
                                    Color(0xB3FDE047), // yellow-300/70
                                    Color(0x8034D399)  // emerald-400/50
                                )
                            )
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Glowing gold badge ₹ icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF78350F)
                                )
                            }

                            Column {
                                Text(
                                    text = celebration.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    letterSpacing = 0.3.sp
                                )

                                if (!celebration.subtitle.isNullOrBlank()) {
                                    Text(
                                        text = celebration.subtitle,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFDE047)
                                    )
                                } else if (celebration.amount != null && celebration.amount > 0) {
                                    Text(
                                        text = "₹${celebration.amount.toInt()} Darj Hua",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE047)
                                    )
                                }
                            }

                            // Subtle status dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = if (celebration.type == CelebrationActionType.UDHAAR_SETTLED) Color(0xFF34D399) else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            } else {
                // REGULAR CELEBRATION (Quick Add / Save / Sync):
                // LAYER 1: EXPANDING SHOCKWAVE HALO RING (w-32 h-32 / 128dp, scale 0.85 -> 1.6, opacity 0.8 -> 0)
                val hp = haloProgress.value
                if (hp in 0.001f..0.999f) {
                    val haloScale = 0.85f + (1.6f - 0.85f) * hp
                    val haloAlpha = ((1f - hp) * 0.80f * masterAlpha.value).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer {
                                scaleX = haloScale
                                scaleY = haloScale
                                alpha = haloAlpha
                            }
                            .border(
                                width = 2.dp,
                                color = Color(0x6634D399), // border-emerald-400/40
                                shape = CircleShape
                            )
                    )
                }

                // LAYER 2: MONEY-RELATED 3D APPLE EMOJI BURST IN THE BACKGROUND
                val burstProgress = (progress / 0.25f).coerceIn(0f, 1f)
                val burstEase = 1f - (1f - burstProgress) * (1f - burstProgress)

                moneyEmojis.forEach { p ->
                    val distPx = with(density) { (p.orbitDistanceDp * burstEase).dp.toPx() }
                    val transX = cos(p.angleRad) * distPx
                    val transY = sin(p.angleRad) * distPx - with(density) { 28.dp.toPx() }

                    val currentScale = if (burstProgress < 0.22f) {
                        (burstProgress / 0.22f) * 1.08f
                    } else {
                        1.0f
                    }

                    Box(
                        modifier = Modifier
                            .size(p.sizeDp.dp)
                            .graphicsLayer {
                                translationX = transX
                                translationY = transY
                                scaleX = currentScale
                                scaleY = currentScale
                                rotationZ = p.spinDeg * burstEase
                                alpha = masterAlpha.value
                            }
                    ) {
                        Money3DEmojiCanvas(kind = p.kind, modifier = Modifier.fillMaxSize())
                    }
                }

                // LAYER 3: 4 STARBURST TWINKLE PARTICLES (Exact 4-point SVG Star Path)
                val currentMs = (progress * 1600).toInt()
                starbursts.forEach { star ->
                    val starElapsed = (currentMs - star.delayMs).coerceAtLeast(0)
                    if (starElapsed > 0 && starElapsed < 850) {
                        val p = starElapsed / 850f
                        // Start at scale 0, burst to scale 1.2, then fade out to 0 over 0.85s
                        val starScale = if (p < 0.25f) (p / 0.25f) * 1.2f else 1.2f - (0.4f * (p - 0.25f) / 0.75f)
                        val starAlpha = (1f - p * p) * masterAlpha.value

                        val transXPx = with(density) { (star.offsetX * (0.85f + 0.15f * p)).dp.toPx() }
                        val transYPx = with(density) { (star.offsetY * (0.85f + 0.15f * p)).dp.toPx() - 28.dp.toPx() }

                        Box(
                            modifier = Modifier
                                .size(star.sizeDp.dp)
                                .graphicsLayer {
                                    translationX = transXPx
                                    translationY = transYPx
                                    scaleX = starScale
                                    scaleY = starScale
                                    alpha = starAlpha
                                }
                        ) {
                            FourPointStarSvgCanvas(color = star.color, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // LAYER 4: CENTERPIECE (Glossy Emerald Medallion + Information Pill Card)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = masterScale.value
                            scaleY = masterScale.value
                            translationY = masterSlideY.value
                        }
                        .padding(horizontal = 24.dp)
                ) {
                    // Outer Ring: Circular badge (w-24 h-24 / 96dp), p-[2.5px], wrapped in metallic gradient
                    // bg-gradient-to-tr from-emerald-400 via-teal-300 to-amber-300 with shadow-emerald-950/40
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF34D399), // emerald-400
                                        Color(0xFF5EEAD4), // teal-300
                                        Color(0xFFFDE047)  // amber-300
                                    ),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            // Inner Capsule: dark luxury emerald core
                            // bg-gradient-to-b from-[#0b3b2c] via-[#06241b] to-[#041912]
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0B3B2C),
                                        Color(0xFF06241B),
                                        Color(0xFF041912)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Specular Glass Reflection: Curved glass reflection rim on upper half
                        // absolute inset-x-2 top-0 h-1/2 bg-gradient-to-b from-white/18 to-transparent rounded-t-full
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 8.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)
                                )
                        )

                        // IN-BADGE SVG VECTOR DRAWING ANIMATION (ViewBox: 0 0 64 64)
                        InBadgeSvgDrawingCanvas(
                            ringDrawProgress = ringDrawProgress.value,
                            checkmarkProgress = checkmarkProgress.value,
                            modifier = Modifier
                                .size(76.dp)
                                .graphicsLayer {
                                    scaleX = checkmarkBounce.value
                                    scaleY = checkmarkBounce.value
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // High-Contrast Frosted Glass Pill Capsule for Transaction Details
                    Surface(
                        color = Color(0xF20B101D),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF34D399),
                                    Color(0xFF5EEAD4),
                                    Color(0xFFFDE047)
                                )
                            )
                        ),
                        shadowElevation = 10.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = celebration.title,
                                fontSize = 16.sp,
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
                                    color = Color(0xFF34D399) // Emerald-400
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
}

/**
 * In-Badge SVG Vector Drawing Animation Canvas (ViewBox: 0 0 64 64):
 * - Static Guide Ring: cx="32" cy="32" r="25" stroke="rgba(52, 211, 153, 0.2)" strokeWidth="2.5".
 * - Animated Drawing Outer Ring: stroke="url(#checkGrad)" strokeWidth="3.5" strokeLinecap="round",
 *   rotate: -90 -> 0, pathLength: 0 -> 1 over 0.42s.
 *   #checkGrad: Linear gradient from #34d399 -> #6ee7b7 -> #fde047.
 * - Crisp Animated White Checkmark: Path "M20 32.5 L28 40.5 L44 23.5",
 *   stroke="#ffffff", strokeWidth="4.5", strokeLinecap="round", strokeLinejoin="round".
 */
@Composable
private fun InBadgeSvgDrawingCanvas(
    ringDrawProgress: Float,
    checkmarkProgress: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = w / 64f // viewBox scale to 64x64

        val cx = 32f * scale
        val cy = 32f * scale
        val r = 25f * scale

        // 1. Static Guide Ring: cx="32" cy="32" r="25" stroke="rgba(52, 211, 153, 0.2)" strokeWidth="2.5"
        drawCircle(
            color = Color(0x3334D399),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f * scale)
        )

        // 2. Animated Drawing Outer Ring:
        // Starts at -90deg (12 o'clock), draws pathLength 0 -> 1 over 0.42s
        if (ringDrawProgress > 0.001f) {
            val sweep = 360f * ringDrawProgress.coerceIn(0f, 1f)
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF34D399), // #34d399
                        Color(0xFF6EE7B7), // #6ee7b7
                        Color(0xFFFDE047)  // #fde047
                    ),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(
                    width = 3.5f * scale,
                    cap = StrokeCap.Round
                )
            )
        }

        // 3. Crisp Animated White Checkmark:
        // Path "M20 32.5 L28 40.5 L44 23.5", stroke="#ffffff", strokeWidth="4.5", strokeLinecap="round", strokeLinejoin="round"
        if (checkmarkProgress > 0.001f) {
            val checkPath = Path().apply {
                moveTo(20f * scale, 32.5f * scale)
                lineTo(28f * scale, 40.5f * scale)
                lineTo(44f * scale, 23.5f * scale)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(checkPath, false)
            val totalLen = pathMeasure.length

            if (totalLen > 0f) {
                val partialPath = Path()
                pathMeasure.getSegment(0f, totalLen * checkmarkProgress.coerceIn(0f, 1f), partialPath, true)

                // Ambient glow stroke under checkmark
                drawPath(
                    path = partialPath,
                    color = Color(0x6634D399),
                    style = Stroke(
                        width = 7f * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Crisp White Checkmark
                drawPath(
                    path = partialPath,
                    color = Color.White,
                    style = Stroke(
                        width = 4.5f * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * 4-Point Starburst Particle SVG Canvas:
 * Exact SVG Path from prompt:
 * "M12 0L14.59 9.41L24 12L14.59 14.59L12 24L9.41 14.59L0 12L9.41 9.41L12 0Z"
 */
@Composable
private fun FourPointStarSvgCanvas(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = w / 24f // Normalized 24x24 viewBox

        val starPath = Path().apply {
            moveTo(12f * s, 0f * s)
            lineTo(14.59f * s, 9.41f * s)
            lineTo(24f * s, 12f * s)
            lineTo(14.59f * s, 14.59f * s)
            lineTo(12f * s, 24f * s)
            lineTo(9.41f * s, 14.59f * s)
            lineTo(0f * s, 12f * s)
            lineTo(9.41f * s, 9.41f * s)
            close()
        }

        // Ambient radial flare
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w / 2f, h / 2f),
                radius = w / 2f
            ),
            radius = w / 2f,
            center = Offset(w / 2f, h / 2f)
        )

        // Solid star fill
        drawPath(starPath, color = color)
        // Crystalline white hot core
        drawCircle(Color.White, radius = 2.5f * s, center = Offset(12f * s, 12f * s))
    }
}

/**
 * Money-Related 3D Apple Emojis Canvas:
 * Authentic 3D Apple lighting, bevels, specular reflections, and drop shadows.
 */
@Composable
private fun Money3DEmojiCanvas(
    kind: MoneyEmojiKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f

        when (kind) {
            // 🪙 3D Apple Gold Coin with Rupee emboss
            MoneyEmojiKind.APPLE_GOLD_COIN -> {
                drawCircle(Color(0x40000000), radius * 0.88f, Offset(cx + 1.5f, cy + 2.5f))
                // 3D Extruded Cylinder Bevel
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFF8C5A00), Color(0xFF5E3A00))),
                    topLeft = Offset(cx - radius * 0.88f, cy - radius * 0.82f + 3f),
                    size = Size(radius * 1.76f, radius * 1.76f)
                )
                // Outer Rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF2A8), Color(0xFFF39C12), Color(0xFFB7791F)),
                        center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                        radius = radius * 0.9f
                    ),
                    radius = radius * 0.88f,
                    center = Offset(cx, cy)
                )
                // Inner Cavity
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF7D6), Color(0xFFE5A10A), Color(0xFF996515)),
                        center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
                        radius = radius * 0.65f
                    ),
                    radius = radius * 0.68f,
                    center = Offset(cx, cy)
                )
                // Indian Rupee (₹) Symbol
                val strokeW = radius * 0.11f
                val embossColor = Color(0xFF5A3C00)
                drawLine(embossColor, Offset(cx - radius * 0.26f, cy - radius * 0.28f), Offset(cx + radius * 0.26f, cy - radius * 0.28f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(embossColor, Offset(cx - radius * 0.26f, cy - radius * 0.10f), Offset(cx + radius * 0.18f, cy - radius * 0.10f), strokeWidth = strokeW * 0.9f, cap = StrokeCap.Round)
                drawLine(embossColor, Offset(cx - radius * 0.08f, cy - radius * 0.28f), Offset(cx - radius * 0.08f, cy + radius * 0.20f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(embossColor, Offset(cx - radius * 0.04f, cy + radius * 0.02f), Offset(cx + radius * 0.22f, cy + radius * 0.30f), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Specular rim shine
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)),
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.78f, cy - radius * 0.78f),
                    size = Size(radius * 1.56f, radius * 1.56f),
                    style = Stroke(width = radius * 0.14f)
                )
            }

            // 💸 3D Apple Flying Cash with Wings
            MoneyEmojiKind.APPLE_FLYING_CASH -> {
                // Feathered Left Wing
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xCCFFFFFF), Color(0x33B2BEC3)),
                        center = Offset(cx - radius * 0.6f, cy - radius * 0.4f),
                        radius = radius * 0.5f
                    ),
                    topLeft = Offset(cx - radius * 0.95f, cy - radius * 0.6f),
                    size = Size(radius * 0.65f, radius * 0.45f)
                )
                // Feathered Right Wing
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xCCFFFFFF), Color(0x33B2BEC3)),
                        center = Offset(cx + radius * 0.6f, cy - radius * 0.4f),
                        radius = radius * 0.5f
                    ),
                    topLeft = Offset(cx + radius * 0.30f, cy - radius * 0.6f),
                    size = Size(radius * 0.65f, radius * 0.45f)
                )
                // Crisp Currency Note Stack
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF55EFC4), Color(0xFF00B894), Color(0xFF006266))),
                    topLeft = Offset(cx - radius * 0.55f, cy - radius * 0.25f),
                    size = Size(radius * 1.1f, radius * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.12f, radius * 0.12f)
                )
                drawCircle(Color(0x66FFFFFF), radius * 0.18f, Offset(cx, cy + radius * 0.05f))
            }

            // 💰 3D Apple Money Bag with Gold Coin Accent
            MoneyEmojiKind.APPLE_MONEY_BAG -> {
                drawCircle(Color(0x35000000), radius * 0.82f, Offset(cx + 1.5f, cy + 3f))
                val bagPath = Path().apply {
                    moveTo(cx - radius * 0.25f, cy - radius * 0.70f)
                    lineTo(cx + radius * 0.25f, cy - radius * 0.70f)
                    lineTo(cx + radius * 0.20f, cy - radius * 0.45f)
                    cubicTo(cx + radius * 0.85f, cy - radius * 0.10f, cx + radius * 0.80f, cy + radius * 0.80f, cx, cy + radius * 0.85f)
                    cubicTo(cx - radius * 0.80f, cy + radius * 0.80f, cx - radius * 0.85f, cy - radius * 0.10f, cx - radius * 0.20f, cy - radius * 0.45f)
                    close()
                }
                // Rich golden-brown bag body
                drawPath(
                    bagPath,
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEAA7), Color(0xFFE67E22), Color(0xFF8C3E00)),
                        center = Offset(cx - radius * 0.2f, cy - radius * 0.1f),
                        radius = radius * 0.9f
                    )
                )
                // Gold Drawstring Tie
                drawLine(Color(0xFFFFD700), Offset(cx - radius * 0.28f, cy - radius * 0.42f), Offset(cx + radius * 0.28f, cy - radius * 0.42f), strokeWidth = radius * 0.10f, cap = StrokeCap.Round)
                // Rupee Symbol on Bag
                drawCircle(Color(0xFF5A3C00), radius * 0.08f, Offset(cx, cy + radius * 0.15f))
            }

            // 💎 3D Apple Ice-Cyan Crystalline Gem
            MoneyEmojiKind.APPLE_DIAMOND_GEM -> {
                val gemPath = Path().apply {
                    moveTo(cx - radius * 0.70f, cy - radius * 0.25f)
                    lineTo(cx - radius * 0.35f, cy - radius * 0.72f)
                    lineTo(cx + radius * 0.35f, cy - radius * 0.72f)
                    lineTo(cx + radius * 0.70f, cy - radius * 0.25f)
                    lineTo(cx, cy + radius * 0.85f)
                    close()
                }
                drawPath(
                    gemPath,
                    Brush.verticalGradient(
                        listOf(Color(0xFFE0FFFF), Color(0xFF00CEC9), Color(0xFF0984E3), Color(0xFF004987))
                    )
                )
                drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - radius * 0.35f, cy - radius * 0.72f), Offset(cx, cy + radius * 0.85f), strokeWidth = 1.8f)
                drawLine(Color.White.copy(alpha = 0.85f), Offset(cx + radius * 0.35f, cy - radius * 0.72f), Offset(cx, cy + radius * 0.85f), strokeWidth = 1.8f)
                drawLine(Color.White.copy(alpha = 0.95f), Offset(cx - radius * 0.70f, cy - radius * 0.25f), Offset(cx + radius * 0.70f, cy - radius * 0.25f), strokeWidth = 1.8f)
                drawCircle(Color.White, radius * 0.14f, Offset(cx - radius * 0.30f, cy - radius * 0.45f))
            }
        }
    }
}

/**
 * In-Badge Mechanical Shred & Cyber Trash Can SVG Animation (ViewBox: 0 0 100 100):
 * 1. Falling & Shredding Document Slip:
 *    - Paper slip: x=35, y=16, w=30, h=38, rx=3 with 4 red text-lines.
 *    - Laser Shred Cut Line: crimson slash path "M 33 34 L 67 36" drawing across paper (delay 0.2s).
 *    - Motion: drops & shrinks into can: y: [-26, -14, 6], scale: [0.9, 0.85, 0.25], rotate: [-12, -4, 6], opacity: [0, 1, 0] over 0.7s.
 * 2. Modern Cyber Trash Can Body:
 *    - Can base path "M 36 50 L 40 82 C 40.5 85 43 87 46 87 L 54 87 C 57 87 59.5 85 60 82 L 64 50 Z" with vertical ribs.
 *    - Rebound Thud: bounces vertically y: [0, 2, -2, 0] at 0.45s as document hits bottom.
 * 3. Dynamic Hinged Lid:
 *    - Lid rim (x=32, y=44, w=36, h=5, rx=2.5) with handle (x=47, y=40, w=6, h=4, rx=1).
 *    - Rotation pivot at hinge (originX: 66px, originY: 48px).
 *    - Open & Snap Physics: rotates open to receive paper, then snaps shut with a tactile bounce: [0, -32, -32, 0, -4, 0] over 0.9s.
 * 4. Vaporize Flash Burst:
 *    - Golden flash circle (cx=50, cy=48, r=12, fill=#FBBF24) bursts (scale: [0, 1.4, 0], opacity: [0, 0.9, 0]) at delay 0.52s.
 */
@Composable
private fun MechanicalShredCanSvgCanvas(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val s = w / 100f // Scale normalized to 100x100 viewBox
        val currentMs = (progress * 1600f).toInt()

        // 1. PAPER SLIP DROP & SHRED MOTION (0ms to 700ms)
        val pDoc = (currentMs / 700f).coerceIn(0f, 1f)
        val docOpacity = if (pDoc < 0.15f) (pDoc / 0.15f)
        else if (pDoc > 0.45f) (1f - (pDoc - 0.45f) / 0.25f).coerceIn(0f, 1f)
        else 1.0f

        val docY = if (pDoc < 0.5f) {
            val t = pDoc / 0.5f
            -26f + (-14f - (-26f)) * t
        } else {
            val t = (pDoc - 0.5f) / 0.5f
            -14f + (6f - (-14f)) * t
        }

        val docScale = if (pDoc < 0.5f) {
            val t = pDoc / 0.5f
            0.9f + (0.85f - 0.9f) * t
        } else {
            val t = (pDoc - 0.5f) / 0.5f
            0.85f + (0.25f - 0.85f) * t
        }

        val docRotate = if (pDoc < 0.5f) {
            val t = pDoc / 0.5f
            -12f + (-4f - (-12f)) * t
        } else {
            val t = (pDoc - 0.5f) / 0.5f
            -4f + (6f - (-4f)) * t
        }

        // Draw Paper Slip if visible
        if (docOpacity > 0.001f) {
            val cxDoc = 50f * s
            val cyDoc = (35f + docY) * s

            rotate(degrees = docRotate, pivot = Offset(cxDoc, cyDoc)) {
                scale(scale = docScale, pivot = Offset(cxDoc, cyDoc)) {
                    val docTopLeft = Offset(35f * s, (16f + docY) * s)
                val docSize = Size(30f * s, 38f * s)

                // Paper Slip Body (white/rose gradient)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = docOpacity),
                            Color(0xFFFDA4AF).copy(alpha = docOpacity * 0.9f)
                        ),
                        startY = docTopLeft.y,
                        endY = docTopLeft.y + docSize.height
                    ),
                    topLeft = docTopLeft,
                    size = docSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s)
                )

                // Paper Slip Border
                drawRoundRect(
                    color = Color(0xFFFB7185).copy(alpha = docOpacity),
                    topLeft = docTopLeft,
                    size = docSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s),
                    style = Stroke(width = 1.2f * s)
                )

                // 4 Red Text Lines
                val lineAlpha = docOpacity * 0.7f
                drawLine(Color(0xFFF43F5E).copy(alpha = lineAlpha), Offset(40f * s, (24f + docY) * s), Offset(58f * s, (24f + docY) * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
                drawLine(Color(0xFFF43F5E).copy(alpha = lineAlpha), Offset(40f * s, (29f + docY) * s), Offset(55f * s, (29f + docY) * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
                drawLine(Color(0xFFF43F5E).copy(alpha = lineAlpha), Offset(40f * s, (34f + docY) * s), Offset(52f * s, (34f + docY) * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
                drawLine(Color(0xFFF43F5E).copy(alpha = lineAlpha), Offset(40f * s, (39f + docY) * s), Offset(48f * s, (39f + docY) * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)

                // Laser Shred Cut Line: d="M 33 34 L 67 36" at delay 0.2s (200ms to 400ms)
                if (currentMs >= 200) {
                    val pCut = ((currentMs - 200) / 200f).coerceIn(0f, 1f)
                    val startX = 33f * s
                    val startY = (34f + docY) * s
                    val endX = 33f * s + (67f - 33f) * s * pCut
                    val endY = (34f + docY) * s + (36f - 34f) * s * pCut

                    drawLine(
                        color = Color(0xFFF43F5E).copy(alpha = docOpacity),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f * s,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

        // 2. REBOUND THUD OF CAN BASE AT 0.45s (450ms to 600ms)
        val reboundY = if (currentMs in 450..600) {
            val pThud = (currentMs - 450) / 150f
            if (pThud < 0.33f) (pThud / 0.33f) * 2f
            else if (pThud < 0.66f) 2f - ((pThud - 0.33f) / 0.33f) * 4f
            else -2f + ((pThud - 0.66f) / 0.34f) * 2f
        } else {
            0f
        }

        // Can Base Path: M 36 50 L 40 82 C 40.5 85 43 87 46 87 L 54 87 C 57 87 59.5 85 60 82 L 64 50 Z
        val canBasePath = Path().apply {
            moveTo(36f * s, (50f + reboundY) * s)
            lineTo(40f * s, (82f + reboundY) * s)
            cubicTo(40.5f * s, (85f + reboundY) * s, 43f * s, (87f + reboundY) * s, 46f * s, (87f + reboundY) * s)
            lineTo(54f * s, (87f + reboundY) * s)
            cubicTo(57f * s, (87f + reboundY) * s, 59.5f * s, (85f + reboundY) * s, 60f * s, (82f + reboundY) * s)
            lineTo(64f * s, (50f + reboundY) * s)
            close()
        }

        drawPath(
            canBasePath,
            Brush.verticalGradient(
                colors = listOf(Color(0xFF2E0914), Color(0xFF140207)),
                startY = (50f + reboundY) * s,
                endY = (87f + reboundY) * s
            )
        )
        drawPath(
            canBasePath,
            color = Color(0xFFFB7185),
            style = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3 Vertical Accent Ribs
        drawLine(Color(0x80FDA4AF), Offset(45f * s, (55f + reboundY) * s), Offset(46f * s, (78f + reboundY) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        drawLine(Color(0x80FDA4AF), Offset(50f * s, (55f + reboundY) * s), Offset(50f * s, (78f + reboundY) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        drawLine(Color(0x80FDA4AF), Offset(55f * s, (55f + reboundY) * s), Offset(54f * s, (78f + reboundY) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)

        // 3. DYNAMIC HINGED LID: Rotates around (66px, 48px)
        // [0, -32, -32, 0, -4, 0] over 0.9s
        val lidAngle = when {
            currentMs < 180 -> 0f
            currentMs in 180..360 -> {
                val t = (currentMs - 180) / 180f
                -32f * sin(t * (PI / 2.0)).toFloat()
            }
            currentMs in 360..480 -> -32f
            currentMs in 480..540 -> {
                val t = (currentMs - 480) / 60f
                -32f + 32f * t
            }
            currentMs in 540..620 -> {
                val t = (currentMs - 540) / 80f
                -4f * sin(t * PI).toFloat()
            }
            else -> 0f
        }

        val pivotX = 66f * s
        val pivotY = 48f * s

        rotate(degrees = lidAngle, pivot = Offset(pivotX, pivotY)) {
            // Lid Rim: x="32" y="44" width="36" height="5" rx="2.5"
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFFFB923C))),
                topLeft = Offset(32f * s, 44f * s),
                size = Size(36f * s, 5f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * s, 2.5f * s)
            )
            drawRoundRect(
                color = Color(0xFFFDA4AF),
                topLeft = Offset(32f * s, 44f * s),
                size = Size(36f * s, 5f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * s, 2.5f * s),
                style = Stroke(width = 1.2f * s)
            )

            // Lid Handle: x="47" y="40" width="6" height="4" rx="1"
            drawRoundRect(
                color = Color(0xFFFDA4AF),
                topLeft = Offset(47f * s, 40f * s),
                size = Size(6f * s, 4f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * s, 1f * s),
                style = Stroke(width = 1.2f * s)
            )
        }

        // 4. VAPORIZE FLASH BURST AT 0.52s (520ms to 680ms)
        if (currentMs in 520..680) {
            val pFlash = (currentMs - 520) / 160f
            val flashScale = if (pFlash < 0.4f) (pFlash / 0.4f) * 1.4f else 1.4f - (1.4f * (pFlash - 0.4f) / 0.6f)
            val flashAlpha = ((sin(pFlash * PI)) * 0.9f).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24).copy(alpha = flashAlpha), Color.Transparent),
                    center = Offset(50f * s, 48f * s),
                    radius = 16f * s * flashScale
                ),
                radius = 16f * s * flashScale,
                center = Offset(50f * s, 48f * s)
            )
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = 4f * s * flashScale,
                center = Offset(50f * s, 48f * s)
            )
        }
    }
}

/**
 * In-Badge SVG Hand & Money Flow Animation (ViewBox: 0 0 84 84):
 * 1. The Cupped Open Hand (Continuously Visible):
 *    - Open palm path: "M16 63 C22 61, 26 57, 31 56 C36 55, 48 55, 54 52 C58 50, 64 47, 68 49 C70 50, 71 52, 69 54 C66 58, 58 63, 50 65 C42 67, 28 68, 18 67 Z"
 *    - Stroke: Golden gradient #handGrad (#fde047 to #f59e0b), strokeWidth="2.5", fill="rgba(245, 158, 11, 0.15)".
 *    - Rises smoothly from y: 8, opacity: 0 to y: 0, opacity: 1 over 0.2s and remains locked in place waiting to receive money.
 * 2. Gliding Currency Note:
 *    - Emerald currency bill (x="23" y="33" width="34" height="19" rx="3.5" fill="url(#cashGrad)" stroke="#ecfdf5" strokeWidth="1.5").
 *    - Features dashed inner security border and bold white centered Rupee symbol (₹).
 *    - Motion: y: -24 -> 0, rotate: -16deg -> -5deg, scale: 0.6 -> 1, opacity: 0 -> 1 (delay 0.12s, stiffness 320, damping 18).
 * 3. Shiny Golden Rupee Coin:
 *    - Golden coin circle (cx="51" cy="38" r="10" fill="url(#coinGrad)" stroke="#fef08a" strokeWidth="1.5"), with inner groove and deep amber ₹ text (#78350f).
 *    - Twinkle Glint: 4-point glint star on coin edge that twinkles scale: [0, 1.2, 0.9] at delay 0.32s.
 *    - Motion: Bounces right on top of note into palm: y: -26 -> 0, x: 5 -> 0, scale: 0.5 -> 1, rotate: -25deg -> 0deg (delay 0.18s, stiffness 340, damping 17).
 */
@Composable
private fun UdhaarHandMoneyFlowSvgCanvas(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val s = w / 84f // Scale relative to 84x84 viewBox
        val currentMs = (progress * 1800f).toInt()

        // 1. THE CUPPED OPEN HAND (Rises smoothly in 200ms and STAYS CONTINUOUSLY VISIBLE throughout!)
        val pHand = (currentMs / 200f).coerceIn(0f, 1f)
        val handY = 8f * (1f - pHand)
        val handOpacity = pHand

        if (handOpacity > 0.001f) {
            val handPath = Path().apply {
                moveTo(16f * s, (63f + handY) * s)
                cubicTo(22f * s, (61f + handY) * s, 26f * s, (57f + handY) * s, 31f * s, (56f + handY) * s)
                cubicTo(36f * s, (55f + handY) * s, 48f * s, (55f + handY) * s, 54f * s, (52f + handY) * s)
                cubicTo(58f * s, (50f + handY) * s, 64f * s, (47f + handY) * s, 68f * s, (49f + handY) * s)
                cubicTo(70f * s, (50f + handY) * s, 71f * s, (52f + handY) * s, 69f * s, (54f + handY) * s)
                cubicTo(66f * s, (58f + handY) * s, 58f * s, (63f + handY) * s, 50f * s, (65f + handY) * s)
                cubicTo(42f * s, (67f + handY) * s, 28f * s, (68f + handY) * s, 18f * s, (67f + handY) * s)
                close()
            }

            // Fill: rgba(245, 158, 11, 0.15)
            drawPath(
                path = handPath,
                color = Color(0xFFF59E0B).copy(alpha = handOpacity * 0.18f)
            )

            // Stroke: Golden gradient #handGrad (#fde047 to #f59e0b), strokeWidth=2.5
            drawPath(
                path = handPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFDE047).copy(alpha = handOpacity),
                        Color(0xFFF59E0B).copy(alpha = handOpacity)
                    )
                ),
                style = Stroke(width = 2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Palm & Finger Accent Contours
            val contourAlpha = handOpacity * 0.65f
            drawLine(
                color = Color(0xFFF59E0B).copy(alpha = contourAlpha),
                start = Offset(32f * s, (57f + handY) * s),
                end = Offset(47f * s, (61f + handY) * s),
                strokeWidth = 1.6f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFDE047).copy(alpha = contourAlpha),
                start = Offset(54f * s, (53f + handY) * s),
                end = Offset(52f * s, (58f + handY) * s),
                strokeWidth = 1.5f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFDE047).copy(alpha = contourAlpha),
                start = Offset(61f * s, (50f + handY) * s),
                end = Offset(59f * s, (55f + handY) * s),
                strokeWidth = 1.5f * s,
                cap = StrokeCap.Round
            )
        }

        // 2. GLIDING CURRENCY NOTE (Delay: 0.12s / 120ms to 480ms)
        if (currentMs >= 120) {
            val pNote = ((currentMs - 120) / 360f).coerceIn(0f, 1f)
            val noteOpacity = (pNote / 0.30f).coerceIn(0f, 1f)
            val noteEase = sin(pNote * (PI / 2.0)).toFloat()
            val noteY = -24f * (1f - noteEase)
            val noteRot = -16f + (11f * noteEase) // -16deg -> -5deg
            val noteScale = 0.6f + (0.4f * noteEase)

            val pivotNote = Offset(40f * s, (42.5f + noteY) * s)
            rotate(degrees = noteRot, pivot = pivotNote) {
                scale(scale = noteScale, pivot = pivotNote) {
                    val billTopLeft = Offset(23f * s, (33f + noteY) * s)
                    val billSize = Size(34f * s, 19f * s)

                    // Emerald currency bill body
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF047857).copy(alpha = noteOpacity),
                                Color(0xFF10B981).copy(alpha = noteOpacity),
                                Color(0xFF34D399).copy(alpha = noteOpacity)
                            )
                        ),
                        topLeft = billTopLeft,
                        size = billSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f * s, 3.5f * s)
                    )

                    // Outer Border: #ecfdf5, strokeWidth 1.5
                    drawRoundRect(
                        color = Color(0xFFECFDF5).copy(alpha = noteOpacity),
                        topLeft = billTopLeft,
                        size = billSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f * s, 3.5f * s),
                        style = Stroke(width = 1.5f * s)
                    )

                    // Inner Dashed Security Border (inset by 2.2)
                    drawRoundRect(
                        color = Color(0xCCECFDF5).copy(alpha = noteOpacity * 0.85f),
                        topLeft = Offset((23f + 2.2f) * s, (33f + noteY + 2.2f) * s),
                        size = Size((34f - 4.4f) * s, (19f - 4.4f) * s),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * s, 2.2f * s),
                        style = Stroke(
                            width = 0.9f * s,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.2f * s, 1.8f * s))
                        )
                    )

                    // Bold White Centered Rupee Symbol (₹)
                    val rupeeAlpha = noteOpacity
                    val rColor = Color.White.copy(alpha = rupeeAlpha)
                    val rY = noteY + 42.5f
                    // Top horizontal bar
                    drawLine(rColor, Offset(37f * s, (rY - 3.2f) * s), Offset(43f * s, (rY - 3.2f) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                    // Mid horizontal bar
                    drawLine(rColor, Offset(37f * s, (rY - 1.2f) * s), Offset(42f * s, (rY - 1.2f) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                    // Stem & Upper curve
                    val rStem = Path().apply {
                        moveTo(38.5f * s, (rY - 3.2f) * s)
                        lineTo(38.5f * s, (rY + 1.2f) * s)
                        cubicTo(41.5f * s, (rY + 1.2f) * s, 42f * s, (rY - 1.2f) * s, 38.5f * s, (rY - 1.2f) * s)
                    }
                    drawPath(rStem, rColor, style = Stroke(width = 1.3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Diagonal leg
                    drawLine(rColor, Offset(38.8f * s, (rY + 0.8f) * s), Offset(43f * s, (rY + 4f) * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                }
            }
        }

        // 3. SHINY GOLDEN RUPEE COIN (Delay: 0.18s / 180ms to 520ms)
        if (currentMs >= 180) {
            val pCoin = ((currentMs - 180) / 340f).coerceIn(0f, 1f)
            val coinOpacity = (pCoin / 0.28f).coerceIn(0f, 1f)
            val coinEase = sin(pCoin * (PI / 2.0)).toFloat()
            val coinY = -26f * (1f - coinEase)
            val coinX = 5f * (1f - coinEase)
            val coinScale = 0.5f + (0.5f * coinEase)
            val coinRotate = -25f * (1f - coinEase)

            val pivotCoin = Offset((51f + coinX) * s, (38f + coinY) * s)
            rotate(degrees = coinRotate, pivot = pivotCoin) {
                scale(scale = coinScale, pivot = pivotCoin) {
                    val coinCenter = Offset((51f + coinX) * s, (38f + coinY) * s)
                    val coinRadius = 10f * s

                    // Golden coin circle fill (#coinGrad)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFEF08A).copy(alpha = coinOpacity),
                                Color(0xFFFBBF24).copy(alpha = coinOpacity),
                                Color(0xFFD97706).copy(alpha = coinOpacity)
                            ),
                            center = coinCenter,
                            radius = coinRadius
                        ),
                        radius = coinRadius,
                        center = coinCenter
                    )

                    // Outer border: #fef08a, strokeWidth 1.5
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = coinOpacity),
                        radius = coinRadius,
                        center = coinCenter,
                        style = Stroke(width = 1.5f * s)
                    )

                    // Inner groove circle: radius 7.5, stroke #b45309
                    drawCircle(
                        color = Color(0xFFB45309).copy(alpha = coinOpacity * 0.45f),
                        radius = 7.5f * s,
                        center = coinCenter,
                        style = Stroke(width = 1f * s)
                    )

                    // Deep Amber Bold Rupee Symbol (#78350f)
                    val cAmber = Color(0xFF78350F).copy(alpha = coinOpacity)
                    val cx = (51f + coinX) * s
                    val cy = (38f + coinY) * s
                    // Top bar
                    drawLine(cAmber, Offset(cx - 3.2f * s, cy - 3.2f * s), Offset(cx + 3.2f * s, cy - 3.2f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                    // Mid bar
                    drawLine(cAmber, Offset(cx - 3.2f * s, cy - 1.2f * s), Offset(cx + 2.4f * s, cy - 1.2f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
                    // Stem & Loop
                    val coinRStem = Path().apply {
                        moveTo(cx - 1.8f * s, cy - 3.2f * s)
                        lineTo(cx - 1.8f * s, cy + 1f * s)
                        cubicTo(cx + 1.8f * s, cy + 1f * s, cx + 2.2f * s, cy - 1.2f * s, cx - 1.8f * s, cy - 1.2f * s)
                    }
                    drawPath(coinRStem, cAmber, style = Stroke(width = 1.3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Diagonal leg
                    drawLine(cAmber, Offset(cx - 1.5f * s, cy + 0.8f * s), Offset(cx + 2.8f * s, cy + 3.8f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)

                    // Twinkle Glint Star on coin edge: delay 0.32s (320ms to 700ms)
                    if (currentMs >= 320) {
                        val pGlint = ((currentMs - 320) / 380f).coerceIn(0f, 1f)
                        if (pGlint < 1f) {
                            val glintScale = if (pGlint < 0.35f) (pGlint / 0.35f) * 1.25f else 1.25f - (0.35f * (pGlint - 0.35f) / 0.65f)
                            val glintAlpha = (sin(pGlint * PI)).toFloat() * coinOpacity
                            val glintCenter = Offset(cx + 6.5f * s, cy - 6.5f * s)

                            // 4-point sparkle star on coin rim
                            val starPath = Path().apply {
                                val rOut = 3.5f * s * glintScale
                                val rIn = 1.1f * s * glintScale
                                moveTo(glintCenter.x, glintCenter.y - rOut)
                                lineTo(glintCenter.x + rIn, glintCenter.y - rIn)
                                lineTo(glintCenter.x + rOut, glintCenter.y)
                                lineTo(glintCenter.x + rIn, glintCenter.y + rIn)
                                lineTo(glintCenter.x, glintCenter.y + rOut)
                                lineTo(glintCenter.x - rIn, glintCenter.y + rIn)
                                lineTo(glintCenter.x - rOut, glintCenter.y)
                                lineTo(glintCenter.x - rIn, glintCenter.y - rIn)
                                close()
                            }
                            drawPath(starPath, Color.White.copy(alpha = glintAlpha))
                            drawPath(starPath, Color(0xFFFEF08A).copy(alpha = glintAlpha * 0.8f), style = Stroke(width = 0.8f * s))
                        }
                    }
                }
            }
        }
    }
}
