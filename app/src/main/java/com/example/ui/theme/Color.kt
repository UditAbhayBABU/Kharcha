package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Dark Slate Surfaces
val BackgroundDark = Color(0xFF05070B)
val SurfaceDark = Color(0xFF090D14)
val SurfaceCard = Color(0xFF0F1522)
val SurfaceElevated = Color(0xFF161E30)
val SurfaceHighlight = Color(0xFF202A42)

// Glassmorphism & Frosted Obsidian Layers
val GlassSurfaceDark = Color(0xF2070A12)
val GlassSurfaceCard = Color(0xE0101626)
val GlassSurfaceHighlight = Color(0x40FFFFFF)
val GlassRimTop = Color(0x66FFFFFF)
val GlassRimBottom = Color(0x1AFFFFFF)

// Borders & Tactile Metallic Edges
val BorderSubtle = Color(0xFF1C2436)
val BorderMedium = Color(0xFF2B374F)
val BorderGlow = Color(0x66F59E0B)
val MetallicGoldRim = Color(0x99F59E0B)

// Shimmering Metallic Border Brushes with Specular Chamfer
val MetallicRimBrush = Brush.linearGradient(
    listOf(
        Color(0x73FFFFFF),
        Color(0x247A8FA6),
        Color(0x55F59E0B),
        Color(0x1A1F2D40)
    )
)

val GoldMetallicRimBrush = Brush.linearGradient(
    listOf(
        Color(0xFFFFFBEB),
        Color(0xFFFDE68A),
        Color(0xFFF59E0B),
        Color(0xFFB45309),
        Color(0x66F59E0B)
    )
)

val EmeraldMetallicRimBrush = Brush.linearGradient(
    listOf(
        Color(0xFFD1FAE5),
        Color(0xFF34D399),
        Color(0xFF059669),
        Color(0x4D10B981)
    )
)

val GlassCardGradient = Brush.verticalGradient(
    listOf(
        Color(0xF5161F32),
        Color(0xFA0B0F1A)
    )
)

// Kharcha Golden & Warm Accents
val GoldPrimary = Color(0xFFF59E0B)
val GoldLight = Color(0xFFFDE68A)
val GoldDark = Color(0xFFB45309)
val AmberVibrant = Color(0xFFFBBF24)

// Functional Accents
val EmeraldCash = Color(0xFF10B981)
val EmeraldDark = Color(0xFF065F46)
val BlueBank = Color(0xFF3B82F6)
val PurpleCard = Color(0xFF8B5CF6)
val OrangeWarning = Color(0xFFF97316)
val RedExpense = Color(0xFFEF4444)

// Typography Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val TextOnGold = Color(0xFF070B14)

